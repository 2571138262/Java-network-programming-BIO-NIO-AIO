package com.baixiaowen.demo;

public class Test {

    public static void main(String[] args) {
        new A(){
            void B_method_new(){
                System.out.println("继承类的匿名内部类 - 调用自定义 - B_method_new方法");
            }
        }.B_method_new();
        new B(){
            @Override
            public void B_method() {
                System.out.println("实现接口的匿名内部类 - B_method");
            }

            void B_method_new() {
                System.out.println("实现接口的匿名内部类的自定义方法 - B_method_new方法");
            }
        }.B_method_new();

        B b = () -> {
            System.out.println("这里调用的是 - 实现接口的匿名内部类 - B_method方法");
        };
        b.B_method();
    }
    
}

class A {}

interface B{
    void B_method();

}
