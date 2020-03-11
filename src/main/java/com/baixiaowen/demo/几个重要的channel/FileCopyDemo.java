package com.baixiaowen.demo.几个重要的channel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 场景:      在本地进行文件的拷贝
 *          1、通过Channel(通道)来进行本地文件的拷贝
 *          2、使用传统的BIO来实现本地文件的拷贝
 */
public class FileCopyDemo {
    
    private static final int ROUNDS = 5;
    
    public static void benchmark(FileCopyRunner test, File source, File targer){
        long elapsed = 0L;
        for (int i = 0; i < ROUNDS; i++) {
            long startTime = System.currentTimeMillis();
            test.copyFile(source, targer);
            long endTime = System.currentTimeMillis();
            elapsed += endTime - startTime;
            targer.delete();
        }
        System.out.println(test + "：" + elapsed / ROUNDS);
    }
    
    public static void close(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        // 使用Stream流进行拷贝  不使用任何缓冲区
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new FileInputStream(source);
                    fout = new FileOutputStream(target);

                    int result;
                    while ((result = fin.read()) != -1){
                        fout.write(result);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);    
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "【noBufferStreamCopy】=== 使用Stream流进行拷贝  不使用任何缓冲区";
            }
        };
        
        // 使用Stream流进行拷贝  使用缓冲区， 先将数据读到缓冲区，然在进行写操作
        FileCopyRunner bufferedStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File targer) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new BufferedInputStream(new FileInputStream(source));
                    fout = new BufferedOutputStream(new FileOutputStream(targer));
                    
                    byte[] buffer = new byte[1024];
                    
                    int result;
                    while ((result = fin.read(buffer)) != -1){
                        fout.write(buffer, 0, result);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "【bufferedStreamCopy】=== 使用Stream流进行拷贝  使用缓冲区， 先将数据读到缓冲区，然在进行写操作";
            }
        };
        
         
        // 使用NIO的Channel来进行拷贝， 依赖Buffer进行数据拷贝
        FileCopyRunner nioBufferCopy = new FileCopyRunner(){
            @Override
            public void copyFile(File source, File targer) {
                FileChannel fin = null;
                FileChannel fout = null;

                try {
                    // 得到了指向输入文件的文件通道
                    fin = new FileInputStream(source).getChannel();
                    // 得到了指向输出文件的文件通道 目标
                    fout = new FileOutputStream(targer).getChannel();

                    // 声明缓冲区并设置大小
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    /**
                     * 当我们对一个目标文件通道进行读操作的时候，实际上是对Buffer进行写操作
                     * 当我们对一个目标文件通道进行写操作的时候，实际上是对Buffer进行读操作
                     */
                    while (fin.read(buffer) != -1){
                        /**
                         * 这里的Buffer的写模式指的是将数据写入到Buffer中，也就是从输入文件通道读取数据到Buffer中
                         * 这里的Buffer的读模式指的是Buffer中读取数据，
                         */
                        
                        /**
                         * 此时要将Buffer从写模式转换为读模式
                         *
                         *  filp() 方法: 
                         *      把position指针放回到Buffer的起始点
                         *     此时limit指针指向所能读到的最远的位置 
                         */
                        buffer.flip();

                        /**
                         * 使用目标文件通道对象开始写操作
                         *      因为fout.write(ByteBuffer src)不能保证一次就把buffer里的所有的数据写入到目标文件中
                         *      所以此处需要通过loop来循环写入
                         *      buffer.hasRemaining() 判断是否还有剩余数据没有被读取出来
                         */
                        while (buffer.hasRemaining()){
                            fout.write(buffer);
                        }

                        /**
                         * 此时把Buffer从读模式转换为写模式
                         */
                        buffer.clear();
                    }
                    
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "【nioBufferCopy】=== 使用NIO的Channel来进行拷贝， 依赖Buffer进行数据拷贝";
            }
        };
        
        // 使用NIO的Channel来进行拷贝， 不依赖Buffer，直接进行Channel间的数据交互
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File targer) {
                // 源文件通道
                FileChannel fin = null;
                // 目标文件通道
                FileChannel fout = null;

                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(targer).getChannel();

                    /**
                     * fin.transferTo(long position, long count, WritableByteChannel target)
                     *  可以把调用的通道中的数据传输到另外的一个通道
                     *  position  -- 开始拷贝数据的位置
                     *  count     -- 要传输多少数据
                     *  target    -- 拷贝文件去的那个目标文件通道
                     *  
                     *  同样这个方法也不能保证一次调用就可以把原通道中所有的数据都传输到目的通道
                     *  所以也要使用while loop
                     */
                    long transferred = 0L;
                    long size = fin.size();
                    while (transferred != size){
                        transferred += fin.transferTo(0, size, fout);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "【nioTransferCopy】=== 使用NIO的Channel来进行拷贝， 不依赖Buffer，直接进行Channel间的数据交互";
            }
        };
        
        
        // 开始测试
        File smallFile = new File("C:\\Users\\Administrator\\Desktop\\smallFile.txt");
        File smallFileCopy = new File("C:\\Users\\Administrator\\Desktop\\smallFileCopy.txt");

        System.out.println("---Copying small file---");
        benchmark(noBufferStreamCopy, smallFile, smallFileCopy);
        benchmark(bufferedStreamCopy, smallFile, smallFileCopy);
        benchmark(nioBufferCopy, smallFile, smallFileCopy);
        benchmark(nioTransferCopy, smallFile, smallFileCopy);

        File bigFile = new File("C:\\Users\\Administrator\\Desktop\\bigFile.txt");
        File bigFileCopy = new File("C:\\Users\\Administrator\\Desktop\\bigFileCopy.txt");

        System.out.println("---Copying big file---");
        benchmark(noBufferStreamCopy, smallFile, smallFileCopy);
        benchmark(bufferedStreamCopy, smallFile, smallFileCopy);
        benchmark(nioBufferCopy, smallFile, smallFileCopy);
        benchmark(nioTransferCopy, smallFile, smallFileCopy);

        File hugeFile = new File("C:\\Users\\Administrator\\Desktop\\hugeFile.txt");
        File hugeFileCopy = new File("C:\\Users\\Administrator\\Desktop\\hugeFileCopy.txt");

        System.out.println("---Copying huge file---");
        benchmark(noBufferStreamCopy, smallFile, smallFileCopy);
        benchmark(bufferedStreamCopy, smallFile, smallFileCopy);
        benchmark(nioBufferCopy, smallFile, smallFileCopy);
        benchmark(nioTransferCopy, smallFile, smallFileCopy);
        
    }
}

/**
 * 本地文件拷贝
 *      源文件到目标文件的拷贝
 */
interface FileCopyRunner{
    void copyFile(File source, File targer);
}

