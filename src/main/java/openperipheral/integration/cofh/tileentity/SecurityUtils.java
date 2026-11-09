package openperipheral.integration.cofh.tileentity;

import com.google.common.base.Preconditions;

import cofh.api.tileentity.ISecurable;
import cofh.api.tileentity.ISecurable.AccessMode;

public class SecurityUtils {

    public static boolean canModify(Object o) {
        if (!(o instanceof ISecurable)) return true;

        final AccessMode access = ((ISecurable) o).getAccess();
        return access != null && access.isPublic();
    }

    public static void checkAccess(Object o) {
        Preconditions.checkArgument(canModify(o), "Access restricted");
    }

}
