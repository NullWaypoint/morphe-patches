package app.nullwaypoint.extension.crunchyroll;

import android.content.Context;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public final class PictureInPicture {
    private static volatile Object manager;
    private static volatile WeakReference<Object> skipViewModel = new WeakReference<>(null);
    private static volatile WeakReference<Context> context = new WeakReference<>(null);
    private static volatile String skipType;
    private static boolean refreshing;

    private PictureInPicture() {
    }

    public static void setManager(Object value) {
        if (value == null) {
            return;
        }

        manager = value;

        Context currentContext = findContext(value);
        if (currentContext != null && context.get() != currentContext) {
            context = new WeakReference<>(currentContext);
        }
    }

    public static void updateSkipState(Object viewModel) {
        if (viewModel == null) {
            return;
        }

        if (skipViewModel.get() != viewModel) {
            skipViewModel = new WeakReference<>(viewModel);
        }

        String currentType = readSkipType(viewModel);
        String previousType = skipType;
        skipType = currentType;

        if (!Objects.equals(previousType, currentType)) {
            refreshActions();
        }
    }

    public static int getForwardIcon(int fallback) {
        if (skipType == null) {
            return fallback;
        }

        return resourceId("drawable", "ic_skip_segment", fallback);
    }

    public static int getForwardText(int fallback) {
        String type = skipType;
        if (type == null) {
            return fallback;
        }

        String name;

        switch (type) {
            case "INTRO":
                name = "player_skip_intro_button_text";
                break;
            case "CREDITS":
                name = "player_skip_credits_button_text";
                break;
            case "PREVIEW":
                name = "player_skip_preview_button_text";
                break;
            case "RECAP":
                name = "player_skip_recap_button_text";
                break;
            default:
                return fallback;
        }

        return resourceId("string", name, fallback);
    }

    public static boolean skipSegmentIfAvailable() {
        Object viewModel = skipViewModel.get();
        if (viewModel == null || currentSkipEvent(viewModel) == null) {
            return false;
        }

        try {
            Method method = findMethod(viewModel.getClass(), "onSkipSegmentClick", 0);
            if (method == null) {
                return false;
            }

            method.setAccessible(true);
            method.invoke(viewModel);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void refreshActions() {
        Object value = manager;
        if (value == null) {
            return;
        }

        synchronized (PictureInPicture.class) {
            if (refreshing) {
                return;
            }

            refreshing = true;
        }

        try {
            for (Method method : value.getClass().getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();

                if (method.getReturnType() != Void.TYPE
                        || parameters.length != 1
                        || parameters[0] != Boolean.TYPE) {
                    continue;
                }

                method.setAccessible(true);
                method.invoke(value, false);
                return;
            }
        } catch (Throwable ignored) {
        } finally {
            synchronized (PictureInPicture.class) {
                refreshing = false;
            }
        }
    }

    private static String readSkipType(Object viewModel) {
        Object event = currentSkipEvent(viewModel);
        if (event == null) {
            return null;
        }

        Class<?> current = event.getClass();

        try {
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    field.setAccessible(true);
                    Object value = field.get(event);

                    if (value instanceof Enum<?>) {
                        return ((Enum<?>) value).name();
                    }
                }

                current = current.getSuperclass();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object currentSkipEvent(Object viewModel) {
        try {
            Method getter = findMethod(viewModel.getClass(), "getSkipEvent", 0);
            if (getter == null) {
                return null;
            }

            getter.setAccessible(true);
            Object state = getter.invoke(viewModel);
            if (state == null) {
                return null;
            }

            Method getValue = findMethod(state.getClass(), "getValue", 0);
            if (getValue == null) {
                return null;
            }

            getValue.setAccessible(true);
            return getValue.invoke(state);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Context findContext(Object value) {
        Class<?> current = value.getClass();

        try {
            while (current != null) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())
                            || !Reference.class.isAssignableFrom(field.getType())) {
                        continue;
                    }

                    field.setAccessible(true);
                    Object reference = field.get(value);

                    if (!(reference instanceof Reference<?>)) {
                        continue;
                    }

                    Object target = ((Reference<?>) reference).get();
                    if (target instanceof Context) {
                        return (Context) target;
                    }
                }

                current = current.getSuperclass();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static int resourceId(String type, String name, int fallback) {
        Context value = context.get();
        if (value == null) {
            return fallback;
        }

        try {
            int id = value.getResources().getIdentifier(
                    name,
                    type,
                    value.getPackageName()
            );

            return id == 0 ? fallback : id;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (name.equals(method.getName())
                    && method.getParameterCount() == parameterCount) {
                return method;
            }
        }

        for (Method method : type.getDeclaredMethods()) {
            if (name.equals(method.getName())
                    && method.getParameterCount() == parameterCount) {
                return method;
            }
        }

        return null;
    }
}
