package com.github.hcsp.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MapBeanConverter {
    // 传入一个遵守Java Bean约定的对象，读取它的所有属性，存储成为一个Map
    // 例如，对于一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 应当返回一个Map { id -> 1, name -> "ABC", longName -> false }
    // 提示：
    //  1. 读取传入参数bean的Class
    //  2. 通过反射获得它包含的所有名为getXXX/isXXX，且无参数的方法（即getter方法）
    //  3. 通过反射调用这些方法并将获得的值存储到Map中返回
    public static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> params = new HashMap<>();
        //获取JavaBean的所有方法进行筛选
        Class<?> executeClass = bean.getClass();
        Method[] methods = getMethods(executeClass);
        try {
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.startsWith("get")) {
                    params.put(methodName.substring(3, 4).toLowerCase() + methodName.substring(4),
                            executeClass.getMethod(methodName).invoke(bean));
                }
                if (methodName.startsWith("is")
                        && methodName.length() > 2
                        && method.invoke(bean) instanceof Boolean) {
                    params.put(methodName.substring(2, 3).toLowerCase() + methodName.substring(3),
                            method.invoke(bean));
                }

            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return params;
    }

    // 传入一个遵守Java Bean约定的Class和一个Map，生成一个该对象的实例
    // 传入参数DemoJavaBean.class和Map { id -> 1, name -> "ABC"}
    // 应当返回一个DemoJavaBean对象 { id = 1, name = "ABC" }
    // 提示：
    //  1. 遍历map中的所有键值对，寻找klass中名为setXXX，且参数为对应值类型的方法（即setter方法）
    //  2. 使用反射创建klass对象的一个实例
    //  3. 使用反射调用setter方法对该实例的字段进行设值
    public static <T> T mapToBean(Class<T> klass, Map<String, Object> map) {
        Object bean = null;
        try {
            bean = klass.getConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String medium = value.getClass().toString();
                //比对参数类型
                String klassParamType = medium.substring(medium.lastIndexOf("s") + 2);
                //需要调用方法名
                String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);

                //获取bean所有方法
                Method[] methods = getMethods(klass);
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        // 对比方法参数类型
                        String callMethodParamType = getMethodParam(method.toString());
                        //参数相同 则进行实例化对象
                        if (klassParamType.equals(callMethodParamType)) {
                            method.invoke(bean, value);
                            break;
                        }
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return (T) bean;
    }

    /**
     * 获取类中所有方法
     *
     * @param bean javaBean
     * @return Method[]
     */
    public static Method[] getMethods(Class<?> bean) {
        return bean.getMethods();
    }

    /**
     * 获取javaBean中方法参数全类名
     *
     * @param methodName 方法名
     * @return 参数类型 全类名
     */
    public static String getMethodParam(String methodName) {
        return methodName.substring(methodName.indexOf("(") + 1, methodName.indexOf(")"));
    }

    public static void main(String[] args) {
        DemoJavaBean bean = new DemoJavaBean();
        bean.setId(100);
        bean.setName("AAAAAAAAAAAAAAAAAAA");
        System.out.println(beanToMap(bean));

        Map<String, Object> map = new HashMap<>();
        map.put("id", 123);
        map.put("name", "ABCDEFG");
        System.out.println(mapToBean(DemoJavaBean.class, map));
    }

    public static class DemoJavaBean {
        private Integer id;
        private String name;
        private String privateField = "privateField";

        public DemoJavaBean() {
        }

        public int isolate() {
            System.out.println(privateField);
            return 0;
        }

        public String is() {
            return "";
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getName(int i) {
            return name + i;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isLongName() {
            return name.length() > 10;
        }

        @Override
        public String toString() {
            return "DemoJavaBean{"
                    + "id="
                    + id
                    + ", name='"
                    + name
                    + '\''
                    + ", longName="
                    + isLongName()
                    + '}';
        }
    }
}
