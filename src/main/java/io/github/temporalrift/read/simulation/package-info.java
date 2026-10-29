/**
 * Execution controls of an isolated simulation deployment: one game per execution context, every inbound record of
 * another game or of a closed context ignored, and the read side discarded when the context closes.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared", "shared::delivery", "shared::inmemory"})
package io.github.temporalrift.read.simulation;
