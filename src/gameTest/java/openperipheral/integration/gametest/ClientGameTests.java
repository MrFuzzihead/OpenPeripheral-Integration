package openperipheral.integration.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import openmods.Log;

/**
 * Client half of the game-test mod: it opens the real config screen and asserts that Forge can construct
 * and display it. CI runs this under Xvfb; the server tests remain the blocking gate.
 */
public class ClientGameTests {

    public static void run() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(new Runnable() {

            @Override
            public void run() {
                try {
                    final GuiScreen screen = new openperipheral.integration.ConfigGuiFactory.ConfigScreen(null);
                    minecraft.displayGuiScreen(screen);
                    if (!(minecraft.currentScreen instanceof openperipheral.integration.ConfigGuiFactory.ConfigScreen)) {
                        throw new AssertionError("Config screen was not displayed");
                    }

                    Log.info("OPENPERIPHERAL_INTEGRATION_CLIENT_GAMETEST_OK");
                    FMLCommonHandler.instance()
                        .exitJava(0, false);
                } catch (Throwable t) {
                    t.printStackTrace();
                    Log.severe(t, "OPENPERIPHERAL_INTEGRATION_CLIENT_GAMETEST_FAILED");
                    FMLCommonHandler.instance()
                        .exitJava(1, false);
                }
            }
        });
    }
}
