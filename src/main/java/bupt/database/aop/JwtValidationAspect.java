package bupt.database.aop;


import bupt.database.util.JwtUtils;
import bupt.database.util.R;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;

@Aspect
@Component
public class JwtValidationAspect {
    private static final String TOKEN_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secret;

    @Around("@annotation(checkJWT)")
    public Object validateToken(ProceedingJoinPoint joinPoint, CheckJWT checkJWT) throws Throwable {
        // 获取HttpServletRequest
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        // 从请求头获取Token
        String tokenHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(tokenHeader) || !tokenHeader.startsWith(TOKEN_PREFIX)) {
            return handleMissingToken(checkJWT);
        }

        String token = tokenHeader.replace(TOKEN_PREFIX, "");

        try {
            // 验证并解析Token
            Claims claims = JwtUtils.parseToken(token, secret);

            // 将用户信息存入请求上下文
            setRequestContext(claims);

            // 注入方法参数（可选）
            injectMethodParameters(joinPoint, claims);

        } catch (ExpiredJwtException e) {
            return R.error( "Token已过期");
        } catch (JwtException e) {
            return R.error("无效Token");
        }

        return joinPoint.proceed();
    }

    private Object handleMissingToken(CheckJWT checkJWT) {
        if (checkJWT.required()) {
            return R.error( "请提供认证Token");
        }
        return null; // 允许匿名访问
    }

    private void setRequestContext(Claims claims) {
        // 可以存入SecurityContextHolder或自定义上下文
        RequestContextHolder.getRequestAttributes()
                .setAttribute("currentUserId", claims.get("userId"), RequestAttributes.SCOPE_REQUEST);
    }

    private void injectMethodParameters(ProceedingJoinPoint joinPoint, Claims claims) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CurrentUserId.class)) {
                args[i] = claims.get("userId", Long.class);
            }
        }
    }
}
