package ru.itmopai.backend.annotation.permission;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.MutableArgumentValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Singleton;
import ru.itmopai.backend.exception.PermissionDeniedException;
import ru.itmopai.backend.service.PermissionService;

import java.util.Arrays;
import java.util.Optional;

@Singleton
public class RequiresPermissionsInterceptor implements MethodInterceptor<Object, Object> {

  private final PermissionService permissionService;

  public RequiresPermissionsInterceptor(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    AnnotationValue<RequiresPermissions> annotation =
        context.getAnnotation(RequiresPermissions.class);
    String[] requiredArray =
        annotation != null
            ? annotation.stringValues("value")
            : new String[0];

    Authentication auth =
        resolveAuth(context)
            .orElseThrow(() -> new RuntimeException("Authentication not found"));

    boolean permitted = false;
    for (String required : requiredArray) {
      String resolvedKey = resolveDynamicObjectId(required, context);
      if (permissionService.hasPermission(auth, resolvedKey)) {
        permitted = true;
        break;
      }
    }
    if (!permitted) {
      throw new PermissionDeniedException(
          "Permission denied for any of: " + String.join(", ", requiredArray));
    }

    return context.proceed();
  }

  private Optional<Authentication> resolveAuth(MethodInvocationContext<?, ?> context) {
    return Arrays.stream(context.getParameterValues())
        .filter(Authentication.class::isInstance)
        .map(Authentication.class::cast)
        .findFirst();
  }

  private String resolveDynamicObjectId(String key, MethodInvocationContext<?, ?> context) {
    if (!key.contains(":"))
      return key;
    String[] parts = key.split(":");
    if (parts.length != 3)
      return key;

    String scope = parts[0];
    String objectIdTemplate = parts[1];
    String priv = parts[2];

    String objectId = objectIdTemplate;
    if (objectIdTemplate.startsWith("{") && objectIdTemplate.endsWith("}")) {
      String paramName = objectIdTemplate.substring(1, objectIdTemplate.length() - 1);
      MutableArgumentValue<?> arg = context.getParameters().get(paramName);
      Object value = arg != null ? arg.getValue() : null;
      if (value != null) {
        objectId = value.toString();
      }
    }

    return scope + ":" + objectId + ":" + priv;
  }
}
