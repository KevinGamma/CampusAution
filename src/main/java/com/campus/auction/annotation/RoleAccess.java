package com.campus.auction.annotation;

import com.campus.auction.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JAX-RS resource method as role-protected.
 * {@link com.campus.auction.filter.JwtSecurityFilter} reads this annotation
 * on every request and rejects callers whose JWT role is not in the allowed set.
 *
 * <pre>
 *   {@literal @}RoleAccess(UserRole.ADMIN)                  // single role
 *   {@literal @}RoleAccess({UserRole.ADMIN, UserRole.STUDENT}) // multiple roles
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleAccess {

    UserRole[] value();
}
