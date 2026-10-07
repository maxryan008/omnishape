package dev.omnishape.api.facade;

import java.util.function.Supplier;

public final class FacadeContext {

    private static final ThreadLocal<Integer> SHAPE_BYPASS =
            ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<Integer> MINING_BYPASS =
            ThreadLocal.withInitial(() -> 0);

    private FacadeContext() {
    }

    public static boolean isShapeBypassed() {
        return SHAPE_BYPASS.get() > 0;
    }

    public static boolean isMiningBypassed() {
        return MINING_BYPASS.get() > 0;
    }

    public static <T> T withoutFacadeShape(
            Supplier<T> supplier
    ) {
        SHAPE_BYPASS.set(
                SHAPE_BYPASS.get() + 1
        );

        try {
            return supplier.get();
        } finally {
            SHAPE_BYPASS.set(
                    SHAPE_BYPASS.get() - 1
            );
        }
    }

    public static <T> T withoutFacadeMining(
            Supplier<T> supplier
    ) {
        MINING_BYPASS.set(
                MINING_BYPASS.get() + 1
        );

        try {
            return supplier.get();
        } finally {
            MINING_BYPASS.set(
                    MINING_BYPASS.get() - 1
            );
        }
    }
}