package com.gongsi.community;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class BlockingTest {

    //直接在main方法中进行测试
    public static void main(String[] args) {
        //定义了阻塞队列的最大接收数据量
        BlockingQueue queue = new ArrayBlockingQueue(10);
        new Thread(new Producer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
    }
}
//一个文件中只能有一个类可以声明权限
//线程需要实现Runnable接口
class Producer implements Runnable {

    private BlockingQueue<Integer> queue;//传进阻塞队列需要有参数接收

    //在实例化生产者这个类的时候就传入阻塞队列，赋值给当前对象的属性字段
    public Producer(BlockingQueue queue) {
        this.queue = queue;
    }

    @Override
    //生产者进程运行需要频繁地往队列中生产数据
    public void run() {
        try {
            for (int i = 0; i < 100; i++) {//设置生产100次数据
                Thread.sleep(20);//每生产一次数据休眠20ms
                queue.put(i);
                System.out.println(Thread.currentThread().getName() + "生产 " + queue.size());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
class Consumer implements Runnable {
    private BlockingQueue queue;
    public Consumer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }
    @Override
    public void run() {
        try {
            //只要有数据就要消费数据
            while(true){
                //不把时间写si，因为进程消费数据有时快有时慢，所以生成0-1000之间的一个数
               Thread.sleep(new Random().nextInt(1000));
               queue.take();
               System.out.println(Thread.currentThread().getName()+"消费 "+queue.size());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
