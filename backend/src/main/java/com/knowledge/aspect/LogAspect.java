package com.knowledge.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.annotation.Log;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 日志切面
 * 自动记录方法执行日志
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LogAspect {
    
    private final ObjectMapper objectMapper;
    
    // 操作日志Logger
    private static final org.slf4j.Logger OPERATION_LOGGER = LoggerFactory.getLogger("OPERATION_LOGGER");
    
    // 敏感信息匹配模式
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(?i)(password|token|secret|key|authorization|credential|pwd|pass)", 
        Pattern.CASE_INSENSITIVE
    );
    
    // 最大日志长度
    private static final int MAX_LOG_LENGTH = 2000;
    
    /**
     * 定义切点：所有带@Log注解的方法
     */
    @Pointcut("@annotation(com.knowledge.annotation.Log)")
    public void logPointcut() {}
    
    /**
     * 环绕通知：记录方法执行日志
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);
        
        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        
        // 构建日志信息
        Map<String, Object> logInfo = buildLogInfo(joinPoint, logAnnotation, request);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录返回结果
            if (logAnnotation.logResult() && result != null) {
                logInfo.put("result", formatResult(result));
            }
            
            logInfo.put("status", "SUCCESS");
            
        } catch (Exception e) {
            exception = e;
            logInfo.put("status", "ERROR");
            logInfo.put("errorMessage", e.getMessage());
            logInfo.put("errorType", e.getClass().getSimpleName());
            
            // 记录异常堆栈（仅前几行）
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length > 0) {
                StringBuilder sb = new StringBuilder();
                int maxLines = Math.min(3, stackTrace.length);
                for (int i = 0; i < maxLines; i++) {
                    sb.append(stackTrace[i].toString()).append("\n");
                }
                logInfo.put("stackTrace", sb.toString());
            }
        } finally {
            // 记录执行时间
            if (logAnnotation.logExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                logInfo.put("executionTime", executionTime);
                logInfo.put("executionTimeStr", executionTime + "ms");
            }
            
            // 异步输出操作日志
            CompletableFuture.runAsync(() -> outputOperationLog(logInfo));
            
            // 控制台输出简要日志
            outputConsoleLog(logAnnotation, logInfo, exception);
        }
        
        if (exception != null) {
            throw exception;
        }
        
        return result;
    }
    
    /**
     * 构建日志信息
     */
    private Map<String, Object> buildLogInfo(ProceedingJoinPoint joinPoint, Log logAnnotation, HttpServletRequest request) {
        Map<String, Object> logInfo = new HashMap<>();
        
        // 基本信息
        logInfo.put("timestamp", System.currentTimeMillis());
        logInfo.put("module", logAnnotation.module());
        logInfo.put("operation", logAnnotation.value());
        logInfo.put("description", logAnnotation.description());
        logInfo.put("method", joinPoint.getSignature().getName());
        logInfo.put("className", joinPoint.getTarget().getClass().getSimpleName());
        
        // 请求信息
        if (request != null) {
            logInfo.put("requestUrl", request.getRequestURL().toString());
            logInfo.put("requestMethod", request.getMethod());
            logInfo.put("userAgent", truncateString(request.getHeader("User-Agent"), 200));
            logInfo.put("ip", getClientIP(request));
            
            // 记录用户信息（如果存在）
            String userId = request.getHeader("X-User-Id");
            if (userId != null) {
                logInfo.put("userId", userId);
            }
        }
        
        // 记录请求参数
        if (logAnnotation.logParams()) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                logInfo.put("params", formatParams(args));
            }
        }
        
        return logInfo;
    }
    
    /**
     * 格式化请求参数
     */
    private String formatParams(Object[] args) {
        try {
            Object[] filteredArgs = filterSensitiveParams(args);
            String paramsStr = objectMapper.writeValueAsString(filteredArgs);
            return truncateString(paramsStr, MAX_LOG_LENGTH);
        } catch (Exception e) {
            log.warn("格式化请求参数失败: {}", e.getMessage());
            return "[格式化失败]";
        }
    }
    
    /**
     * 格式化返回结果
     */
    private String formatResult(Object result) {
        try {
            String resultStr = objectMapper.writeValueAsString(result);
            return truncateString(resultStr, MAX_LOG_LENGTH);
        } catch (Exception e) {
            log.warn("格式化返回结果失败: {}", e.getMessage());
            return "[格式化失败]";
        }
    }
    
    /**
     * 异步输出操作日志
     */
    private void outputOperationLog(Map<String, Object> logInfo) {
        try {
            String logJson = objectMapper.writeValueAsString(logInfo);
            OPERATION_LOGGER.info(logJson);
        } catch (Exception e) {
            log.error("输出操作日志失败: {}", e.getMessage());
        }
    }
    
    /**
     * 输出控制台日志
     */
    private void outputConsoleLog(Log logAnnotation, Map<String, Object> logInfo, Exception exception) {
        String module = logAnnotation.module();
        String operation = logAnnotation.value();
        String executionTime = (String) logInfo.get("executionTimeStr");
        
        if (exception == null) {
            log.info("✓ {} - {} - {}", module, operation, executionTime);
        } else {
            log.error("✗ {} - {} - {} - {}", module, operation, executionTime, exception.getMessage());
        }
    }
    
    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 处理多IP的情况，取第一个IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 过滤敏感参数
     */
    private Object[] filterSensitiveParams(Object[] args) {
        if (args == null) {
            return null;
        }
        
        Object[] filteredArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                filteredArgs[i] = filterSensitiveObject(args[i]);
            } else {
                filteredArgs[i] = null;
            }
        }
        return filteredArgs;
    }
    
    /**
     * 过滤敏感对象
     */
    private Object filterSensitiveObject(Object obj) {
        if (obj == null) {
            return null;
        }
        
        String objStr = obj.toString();
        
        // 检查是否包含敏感信息
        if (SENSITIVE_PATTERN.matcher(objStr).find()) {
            return "[FILTERED]";
        }
        
        // 对于Map类型，递归过滤
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Map<Object, Object> filteredMap = new HashMap<>();
            
            map.forEach((key, value) -> {
                String keyStr = key != null ? key.toString().toLowerCase() : "";
                if (SENSITIVE_PATTERN.matcher(keyStr).find()) {
                    filteredMap.put(key, "[FILTERED]");
                } else {
                    filteredMap.put(key, filterSensitiveObject(value));
                }
            });
            
            return filteredMap;
        }
        
        return obj;
    }
    
    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength) + "...[截断]";
    }
} 