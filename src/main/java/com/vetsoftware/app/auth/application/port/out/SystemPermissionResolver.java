package com.vetsoftware.app.auth.application.port.out;

import java.util.Set;

public interface SystemPermissionResolver {
  Set<String> resolveFor(Long systemUserId);
}
