package openperipheral.integration.railcraft;

import openmods.reflection.MethodAccess;
import openmods.reflection.MethodAccess.Function0;
import openmods.reflection.ReflectionHelper;
import openperipheral.api.adapter.IPeripheralAdapter;

/**
 * Base for Railcraft's BuildCraft trigger interfaces: they all expose one no-argument boolean method on a
 * class that only exists when Railcraft is installed. Keeps the reflection setup in one place.
 */
abstract class BooleanReflectionAdapter implements IPeripheralAdapter {

    private final String sourceId;
    private final Class<?> targetClass;
    private final Function0<Boolean> method;

    protected BooleanReflectionAdapter(String sourceId, String className, String methodName) {
        this.sourceId = sourceId;
        this.targetClass = ReflectionHelper.getClass(className);
        this.method = MethodAccess.create(boolean.class, targetClass, methodName);
    }

    @Override
    public Class<?> getTargetClass() {
        return targetClass;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    protected boolean callBoolean(Object target) {
        return method.call(target);
    }
}
