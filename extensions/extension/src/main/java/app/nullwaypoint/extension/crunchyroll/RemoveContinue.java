package app.nullwaypoint.extension.crunchyroll;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class RemoveContinue {
    private static final String PANEL_CLASS = "com.ellation.crunchyroll.model.Panel";
    private static final String PREFS_NAME = "nullwaypoint_remove_continue";
    private static final String HIDDEN_PLAYHEAD_PREFIX = "hidden_playhead:";
    private static final Object REMOVE_CONTAINER = new Object();
    private static final ThreadLocal<Object> currentData = new ThreadLocal<>();

    private static volatile Context appContext;
    private static volatile SharedPreferences preferences;

    private RemoveContinue() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }

        appContext = context;
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void captureData(Object data) {
        currentData.set(data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List addRemoveContinue(List original, Object provider) {
        Object data = currentData.get();
        currentData.remove();

        if (original == null || data == null) {
            return original;
        }

        try {
            Object menuLabel = getRemoveMenuLabel(original);
            if (menuLabel == null) {
                return original;
            }

            Class<?> menuItemClass = original.isEmpty()
                    ? Class.forName("qs0.h")
                    : original.get(0).getClass();

            Constructor<?> constructor = findMenuItemConstructor(menuItemClass, menuLabel);
            if (constructor == null) {
                return original;
            }

            Class<?> callbackType = constructor.getParameterTypes()[1];
            Object callback = Proxy.newProxyInstance(
                    callbackType.getClassLoader(),
                    new Class<?>[]{callbackType},
                    new MenuClickHandler(data, provider)
            );

            Object removeItem = constructor.newInstance(menuLabel, callback);
            ArrayList result = new ArrayList(original.size() + 1);
            result.addAll(original);
            result.add(removeItem);
            return result;
        } catch (Throwable ignored) {
            return original;
        }
    }

    public static void filterFeed(Object feed) {
        SharedPreferences prefs = preferences;
        if (prefs == null || feed == null) {
            return;
        }

        try {
            List<?> items = findItems(feed);
            if (items == null || items.isEmpty()) {
                return;
            }

            Iterator<?> iterator = items.iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();

                try {
                    String contentId = getContentId(item);
                    if (contentId == null) {
                        continue;
                    }

                    String key = HIDDEN_PLAYHEAD_PREFIX + contentId;
                    if (!prefs.contains(key)) {
                        continue;
                    }

                    long hiddenPlayhead = prefs.getLong(key, Long.MIN_VALUE);
                    Long currentPlayhead = getPlayheadSec(item);

                    if (currentPlayhead != null
                            && currentPlayhead.longValue() != hiddenPlayhead) {
                        prefs.edit().remove(key).apply();
                        continue;
                    }

                    iterator.remove();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class MenuClickHandler implements InvocationHandler {
        private final Object data;
        private final Object provider;

        private MenuClickHandler(Object data, Object provider) {
            this.data = data;
            this.provider = provider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if ("invoke".equals(method.getName())) {
                hide(data, provider);
                return null;
            }

            return defaultValue(method.getReturnType());
        }
    }

    private static void hide(Object data, Object provider) {
        SharedPreferences prefs = preferences;
        if (prefs == null) {
            return;
        }

        try {
            String contentId = getContentId(data);
            if (contentId == null || contentId.isEmpty()) {
                return;
            }

            Long playhead = getPlayheadSec(data);
            if (playhead == null) {
                return;
            }

            prefs.edit()
                    .putLong(HIDDEN_PLAYHEAD_PREFIX + contentId, playhead)
                    .apply();

            removeFromCurrentFeed(provider, contentId);
        } catch (Throwable ignored) {
        }
    }

    private static boolean removeFromCurrentFeed(Object provider, String contentId) {
        try {
            Object controller = findHomeFeedController(provider);
            if (controller == null) {
                return false;
            }

            Method getState = findMethod(controller.getClass(), "getState", 0);
            if (getState == null) {
                return false;
            }

            getState.setAccessible(true);
            Object stateFlow = getState.invoke(controller);
            if (stateFlow == null) {
                return false;
            }

            Method getValue = findMethod(stateFlow.getClass(), "getValue", 0);
            Method setValue = findMethod(stateFlow.getClass(), "setValue", 1);
            if (getValue == null || setValue == null) {
                return false;
            }

            getValue.setAccessible(true);
            setValue.setAccessible(true);

            Object state = getValue.invoke(stateFlow);
            if (state == null) {
                return false;
            }

            Object newState = copyStateWithoutContent(state, contentId);
            if (newState == null) {
                return false;
            }

            setValue.invoke(stateFlow, newState);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object findHomeFeedController(Object provider) throws Exception {
        if (provider == null) {
            return null;
        }

        for (Field providerField : provider.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(providerField.getModifiers())) {
                continue;
            }

            providerField.setAccessible(true);
            Object mediator = providerField.get(provider);
            if (mediator == null) {
                continue;
            }

            for (Field mediatorField : mediator.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(mediatorField.getModifiers())) {
                    continue;
                }

                mediatorField.setAccessible(true);
                Object candidate = mediatorField.get(mediator);
                if (candidate != null
                        && findMethod(candidate.getClass(), "getState", 0) != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static Object copyStateWithoutContent(Object state, String contentId) throws Exception {
        Field feedField = null;
        List<?> feed = null;

        for (Field field : state.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    || !List.class.isAssignableFrom(field.getType())) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(state);
            if (value instanceof List) {
                feedField = field;
                feed = (List<?>) value;
                break;
            }
        }

        if (feedField == null || feed == null) {
            return null;
        }

        ArrayList<Object> updatedFeed = new ArrayList<>(feed);
        boolean changed = false;

        for (int i = 0; i < updatedFeed.size(); i++) {
            Object replacement = copyEntryWithoutContent(updatedFeed.get(i), contentId);
            if (replacement == null) {
                continue;
            }

            changed = true;
            if (replacement == REMOVE_CONTAINER) {
                updatedFeed.remove(i--);
            } else {
                updatedFeed.set(i, replacement);
            }
        }

        return changed
                ? copyObjectReplacingField(state, feedField, updatedFeed)
                : null;
    }

    private static Object copyEntryWithoutContent(Object entry, String contentId) throws Exception {
        if (entry == null) {
            return null;
        }

        for (Field field : entry.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    || !List.class.isAssignableFrom(field.getType())) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(entry);
            if (!(value instanceof List)) {
                continue;
            }

            List<?> items = (List<?>) value;
            ArrayList<Object> updatedItems = new ArrayList<>(items.size());
            boolean removed = false;

            for (Object item : items) {
                String itemId = null;

                try {
                    itemId = getContentId(item);
                } catch (Throwable ignored) {
                }

                if (contentId.equals(itemId)) {
                    removed = true;
                } else {
                    updatedItems.add(item);
                }
            }

            if (!removed) {
                continue;
            }

            return updatedItems.isEmpty()
                    ? REMOVE_CONTAINER
                    : copyObjectReplacingField(entry, field, updatedItems);
        }

        return null;
    }

    private static Object copyObjectReplacingField(
            Object source,
            Field replacedField,
            Object replacement
    ) throws Exception {
        Field[] fields = Arrays.stream(source.getClass().getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !field.isSynthetic())
                .sorted(Comparator.comparing(Field::getName))
                .toArray(Field[]::new);

        for (Field field : fields) {
            field.setAccessible(true);
        }

        for (Constructor<?> constructor : source.getClass().getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != fields.length) {
                continue;
            }

            Object[] arguments = new Object[fields.length];
            boolean compatible = true;

            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                Object value = field.equals(replacedField)
                        ? replacement
                        : field.get(source);

                if (!isCompatible(parameterTypes[i], field.getType(), value)) {
                    compatible = false;
                    break;
                }

                arguments[i] = value;
            }

            if (compatible) {
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            }
        }

        return null;
    }

    private static boolean isCompatible(
            Class<?> parameterType,
            Class<?> fieldType,
            Object value
    ) {
        if (parameterType.equals(fieldType)) {
            return true;
        }

        if (value == null) {
            return !parameterType.isPrimitive();
        }

        return !parameterType.isPrimitive() && parameterType.isInstance(value);
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

    private static List<?> findItems(Object feed) throws Exception {
        Class<?> current = feed.getClass();

        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || !List.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(feed);
                if (value instanceof List) {
                    return (List<?>) value;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static Long getPlayheadSec(Object value) throws Exception {
        if (value == null) {
            return null;
        }

        Method getter = findMethod(value.getClass(), "getPlayheadSec", 0);
        if (getter != null
                && (getter.getReturnType() == Long.TYPE
                || getter.getReturnType() == Long.class)) {
            getter.setAccessible(true);
            Object result = getter.invoke(value);
            if (result instanceof Long) {
                return (Long) result;
            }
        }

        Field playheadField = null;
        Class<?> current = value.getClass();

        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.getType() != Long.TYPE) {
                    continue;
                }

                if (playheadField != null) {
                    return null;
                }

                playheadField = field;
            }

            current = current.getSuperclass();
        }

        if (playheadField == null) {
            return null;
        }

        playheadField.setAccessible(true);
        return playheadField.getLong(value);
    }

    private static String getContentId(Object data) throws Exception {
        Object panel = findPanel(data, 3, new IdentityHashMap<>());
        if (panel == null) {
            return null;
        }

        Method getId = panel.getClass().getMethod("getId");
        Object id = getId.invoke(panel);
        return id instanceof String ? (String) id : null;
    }

    private static Object findPanel(
            Object value,
            int depth,
            Map<Object, Boolean> visited
    ) throws Exception {
        if (value == null || depth < 0) {
            return null;
        }

        if (PANEL_CLASS.equals(value.getClass().getName())) {
            return value;
        }

        if (visited.put(value, Boolean.TRUE) != null) {
            return null;
        }

        for (Method method : value.getClass().getMethods()) {
            if (method.getParameterCount() != 0
                    || !PANEL_CLASS.equals(method.getReturnType().getName())) {
                continue;
            }

            method.setAccessible(true);
            Object panel = method.invoke(value);
            if (panel != null) {
                return panel;
            }
        }

        if (depth == 0) {
            return null;
        }

        Class<?> current = value.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.getType().isPrimitive()) {
                    continue;
                }

                field.setAccessible(true);
                Object nested = field.get(value);
                if (nested == null) {
                    continue;
                }

                if (PANEL_CLASS.equals(nested.getClass().getName())) {
                    return nested;
                }

                String className = nested.getClass().getName();
                if (className.startsWith("java.")
                        || className.startsWith("kotlin.")
                        || className.startsWith("android.")) {
                    continue;
                }

                Object panel = findPanel(nested, depth - 1, visited);
                if (panel != null) {
                    return panel;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static Object getRemoveMenuLabel(List<?> original) throws Exception {
        Context context = appContext;
        if (context == null) {
            return null;
        }

        ClassLoader loader = original.isEmpty()
                ? RemoveContinue.class.getClassLoader()
                : original.get(0).getClass().getClassLoader();

        Class<?> labelClass = Class.forName("cd0.i", false, loader);
        Object template = null;

        for (Field field : labelClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    || !labelClass.isAssignableFrom(field.getType())) {
                continue;
            }

            field.setAccessible(true);
            template = field.get(null);
            if (template != null) {
                break;
            }
        }

        if (template == null) {
            return null;
        }

        int removeText = context.getResources().getIdentifier(
                "remove",
                "string",
                context.getPackageName()
        );

        if (removeText == 0) {
            return template;
        }

        Class<?> baseClass = labelClass.getSuperclass();
        Integer color = null;
        Integer trailing = null;
        boolean flag = false;

        for (Field field : baseClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            if (field.getType() == Integer.class) {
                Integer value = (Integer) field.get(template);
                if (value != null && color == null) {
                    color = value;
                } else {
                    trailing = value;
                }
            } else if (field.getType() == Boolean.TYPE) {
                flag = field.getBoolean(template);
            }
        }

        Constructor<?> constructor = baseClass.getDeclaredConstructor(
                Integer.TYPE,
                Integer.class,
                Boolean.TYPE,
                Integer.class
        );

        constructor.setAccessible(true);
        return constructor.newInstance(removeText, color, flag, trailing);
    }

    private static Constructor<?> findMenuItemConstructor(
            Class<?> menuItemClass,
            Object menuLabel
    ) {
        for (Constructor<?> constructor : menuItemClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();

            if (parameters.length == 2
                    && parameters[0].isInstance(menuLabel)
                    && parameters[1].isInterface()) {
                constructor.setAccessible(true);
                return constructor;
            }
        }

        return null;
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "toString":
                return "NullWaypointRemoveContinueCallback";
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return args != null && args.length == 1 && proxy == args[0];
            default:
                return null;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }

        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Character.TYPE) {
            return '\0';
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0f;
        }
        if (type == Double.TYPE) {
            return 0d;
        }

        return null;
    }
}
