/**
 * Stable third-party extension API for AutoTest.
 *
 * <p>Types in this package tree are public compatibility surface. Plugin instances are discovered
 * with {@link java.util.ServiceLoader}, may be reused across compilations, and must be stateless or
 * thread-safe unless a lifecycle contract explicitly says otherwise. Implementations must not
 * retain secrets or mutable execution state after a run ends.
 *
 * @since 1.0
 */
package org.openbear.tool.autotest.spi;
