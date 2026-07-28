package com.nore.cobblebash.integration;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

public final class CobbleDollarsCompat {
    private static final String MOD_ID = "cobbledollars";
    private static final String PLAYER_EXTENSIONS = "fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt";
    private static Method getCobbleDollarsMethod;
    private static Method setCobbleDollarsMethod;
    private static boolean unavailableLogged;

    private CobbleDollarsCompat() {
    }

    public static boolean award(ServerPlayer player, int amount) {
        if (amount <= 0 || !ModList.get().isLoaded(MOD_ID) || !ensureMethodsLoaded()) {
            return false;
        }

        try {
            BigInteger current = (BigInteger) getCobbleDollarsMethod.invoke(null, player);
            BigInteger reward = BigInteger.valueOf(amount);
            setCobbleDollarsMethod.invoke(null, player, current.add(reward));
            return true;
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            logUnavailable(exception);
            return false;
        }
    }

    private static boolean ensureMethodsLoaded() {
        if (getCobbleDollarsMethod != null && setCobbleDollarsMethod != null) {
            return true;
        }

        try {
            Class<?> extensions = Class.forName(PLAYER_EXTENSIONS);
            getCobbleDollarsMethod = extensions.getMethod("getCobbleDollars", net.minecraft.world.entity.player.Player.class);
            setCobbleDollarsMethod = extensions.getMethod("setCobbleDollars", net.minecraft.world.entity.player.Player.class, BigInteger.class);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            logUnavailable(exception);
            return false;
        }
    }

    private static void logUnavailable(Exception exception) {
        if (unavailableLogged) {
            return;
        }

        unavailableLogged = true;
        CobbleBash.LOGGER.warn("Cobble Dollars is loaded, but CobbleBash could not access its balance API.", exception);
    }
}
