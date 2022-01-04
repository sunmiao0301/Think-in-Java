Java最出色的一个地方就是它的“无痛苦连网”概念

15.1 机器的标识

15.1.1 服务器和客户机

1.在没有网络的前提下测试程序
1)
/*
如果向getByName()传递一个null（空）值，就默认为使用localhost
localhost来满足非网络环境中的测试要求。
*/
InetAddress addr = InetAddress.getByName(null);
2)
/*
为得到本地主机地址，亦可向其直接传递字串"localhost"：
*/
InetAddress.getByName("localhost");
3)
/*
或者使用它的保留IP地址（四点形式）
*/
InetAddress.getByName("127.0.0.1");

15.1.2 端口：机器内独一无二的场所
----有些时候，一个IP地址并不足以完整标识一个服务器。这是由于在一台物理性的机器中，往往运行着多个服务器（程序）
就象我们去拜会某人时，IP地址是他居住的房子，而端口是他在的那个房间。
系统服务保留了使用端口1到端口1024的权力，所以不应让自己设计的服务占用这些以及其他任何已知正在使用的端口。

15.2 套接字

15.3 服务多个客户
JabberServer可以正常工作，但每次只能为一个客户程序提供服务。
在典型的服务器中，我们希望同时能处理多个客户的请求。
解决这个问题的关键就是多线程处理机制。-----
最基本的方法是在服务器（程序）里创建单个ServerSocket，并调用 accept()来等候一个新连接。
一旦 accept()返回，我们就取得结果获得的Socket，并用它新建一个线程，令其只为那个特定的客户服务。
然后再调用 accept()，等候下一次新的连接请求。
如下服务器代码：
//: MultiJabberServer.java
// A server that uses multithreading to handle 
// any number of clients.
import java.io.*;
import java.net.*;

class ServeOneJabber extends Thread {
  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  public ServeOneJabber(Socket s) 
      throws IOException {
    socket = s;
    in = 
      new BufferedReader(
        new InputStreamReader(
          socket.getInputStream()));
    // Enable auto-flush:
    out = 
      new PrintWriter(
        new BufferedWriter(
          new OutputStreamWriter(
            socket.getOutputStream())), true);
    // If any of the above calls throw an 
    // exception, the caller is responsible for
    // closing the socket. Otherwise the thread
    // will close it.
    start(); // Calls run()
  }
  public void run() {
    try {
      while (true) {  
        String str = in.readLine();
        if (str.equals("END")) break;
        System.out.println("Echoing: " + str);
        out.println(str);
      }
      System.out.println("closing...");
    } catch (IOException e) {
    } finally {
      try {
        socket.close();
      } catch(IOException e) {}
    }
  }
}

public class MultiJabberServer {  
  static final int PORT = 8080;
  public static void main(String[] args)
      throws IOException {
    /*
    请注意MultiJabberServer有多么简单。
    和以前一样，我们创建一个ServerSocket，并调用accept()允许一个新连接的建立。
    但这一次，accept()的返回值（一个套接字）将传递给用于ServeOneJabber的构建器，由它创建一个新线程，并对那个连接进行控制。
    连接中断后，线程便可简单地消失。
    */
    ServerSocket s = new ServerSocket(PORT);
    System.out.println("Server Started");
    /*
    如果ServerSocket创建失败，则再一次通过main()掷出违例。
    如果成功，则位于外层的try-finally代码块可以担保正确的清除。
    位于内层的try-catch块只负责防范ServeOneJabber构建器的失败；
    若构建器成功，则ServeOneJabber线程会将对应的套接字关掉。
    */
    try {
      while(true) {
        // Blocks until a connection occurs:
        Socket socket = s.accept();
        try {
          new ServeOneJabber(socket);
        } catch(IOException e) {
          // If it fails, close the socket,
          // otherwise the thread will close it:
          socket.close();
        }
      }
    } finally {
      s.close();
    }
  } 
} ///:~
为了证实服务器代码确实能为多名客户提供服务，下面这个程序将创建许多客户（使用线程），并同相同的服务器建立连接。
每个线程的“存在时间”都是有限的。一旦到期，就留出空间以便创建一个新线程。
//: MultiJabberClient.java
// Client that tests the MultiJabberServer
// by starting up multiple clients.
import java.net.*;
import java.io.*;

class JabberClientThread extends Thread {
  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private static int counter = 0;
  private int id = counter++;
  private static int threadcount = 0;
  public static int threadCount() { 
    return threadcount; 
  }
  public JabberClientThread(InetAddress addr) {
    System.out.println("Making client " + id);
    threadcount++;
    try {
      socket = 
        new Socket(addr, MultiJabberServer.PORT);
    } catch(IOException e) {
      // If the creation of the socket fails, 
      // nothing needs to be cleaned up.
    }
    try {    
      in = 
        new BufferedReader(
          new InputStreamReader(
            socket.getInputStream()));
      // Enable auto-flush:
      out = 
        new PrintWriter(
          new BufferedWriter(
            new OutputStreamWriter(
              socket.getOutputStream())), true);
      start();
    } catch(IOException e) {
      // The socket should be closed on any 
      // failures other than the socket 
      // constructor:
      try {
        socket.close();
      } catch(IOException e2) {}
    }
    // Otherwise the socket will be closed by
    // the run() method of the thread.
  }
  public void run() {
    try {
      for(int i = 0; i < 25; i++) {
        out.println("Client " + id + ": " + i);
        String str = in.readLine();
        System.out.println(str);
      }
      out.println("END");
    } catch(IOException e) {
    } finally {
      // Always close it:
      try {
        socket.close();
      } catch(IOException e) {}
      threadcount--; // Ending this thread
    }
  }
}

public class MultiJabberClient {
  static final int MAX_THREADS = 40;
  public static void main(String[] args) 
      throws IOException, InterruptedException {
    InetAddress addr = 
      InetAddress.getByName(null);
    while(true) {
      if(JabberClientThread.threadCount() 
         < MAX_THREADS)
        new JabberClientThread(addr);
      Thread.currentThread().sleep(100);
    }
  }
} ///:~

15.4 数据报 —— 
大家迄今看到的例子使用的都是“传输控制协议”（TCP），亦称作“基于 **数据流** 的套接字”。优缺点见计算机网络
还有另一种协议，名为“用户 **数据报** 协议”（UDP）

15.5 一个Web应用 —— unseen

15.5.1 服务器应用

15.6 Java与CGI的沟通

15.7 用JDBC连接数据库
Java为自己保证的一项出色能力就是构建与平台无关的客户机／服务器数据库应用。
在Java 1.1中，这一保证通过Java数据库连接（JDBC）实现了。

15.8 远程方法 —— “远程方法调用”（RMI）

15.9 总结

15.10 练习
(1) 编译和运行本章中的JabberServer和JabberClient程序。接着编辑一下程序，删去为输入和输出设计的所有缓冲机制，然后再次编译和运行，观察一下结果。

(2) 创建一个服务器，用它请求用户输入密码，然后打开一个文件，并将文件通过网络连接传送出去。创建一个同该服务器连接的客户，为其分配适当的密码，然后捕获和保存文件。在自己的机器上用localhost（通过调用InetAddress.getByName(null)生成本地IP地址127.0.0.1）测试这两个程序。

(3) 修改练习2中的程序，令其用多线程机制对多个客户进行控制。

(4) 修改JabberClient，禁止输出刷新，并观察结果。

(5) 以ShowHTML.java为基础，创建一个程序片，令其成为对自己Web站点的特定部分进行密码保护的大门。

(6) （可能有些难度）创建一对客户／服务器程序，利用数据报（Datagram）将一个文件从一台机器传到另一台（参见本章数据报小节末尾的叙述）。

(7) （可能有些难度）对VLookup.java程序作一番修改，使我们能点击得到的结果名字，然后程序会自动取得那个名字，并把它复制到剪贴板（以便我们方便地粘贴到自己的E-mail）。可能要回过头去研究一下IO数据流的那一章，回忆该如何使用Java 1.1剪贴板。

