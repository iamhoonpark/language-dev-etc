package com.eomcs.pms;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import com.eomcs.pms.dao.BoardDao;
import com.eomcs.pms.dao.MemberDao;
import com.eomcs.pms.dao.ProjectDao;
import com.eomcs.pms.dao.TaskDao;
import com.eomcs.pms.dao.mariadb.BoardDaoImpl;
import com.eomcs.pms.dao.mariadb.MemberDaoImpl;
import com.eomcs.pms.dao.mariadb.ProjectDaoImpl;
import com.eomcs.pms.dao.mariadb.TaskDaoImpl;
import com.eomcs.pms.handler.BoardAddHandler;
import com.eomcs.pms.handler.BoardDeleteHandler;
import com.eomcs.pms.handler.BoardDetailHandler;
import com.eomcs.pms.handler.BoardListHandler;
import com.eomcs.pms.handler.BoardSearchHandler;
import com.eomcs.pms.handler.BoardUpdateHandler;
import com.eomcs.pms.handler.Command;
import com.eomcs.pms.handler.MemberAddHandler;
import com.eomcs.pms.handler.MemberDeleteHandler;
import com.eomcs.pms.handler.MemberDetailHandler;
import com.eomcs.pms.handler.MemberListHandler;
import com.eomcs.pms.handler.MemberUpdateHandler;
import com.eomcs.pms.handler.MemberValidator;
import com.eomcs.pms.handler.ProjectAddHandler;
import com.eomcs.pms.handler.ProjectDeleteHandler;
import com.eomcs.pms.handler.ProjectDetailHandler;
import com.eomcs.pms.handler.ProjectDetailSearchHandler;
import com.eomcs.pms.handler.ProjectListHandler;
import com.eomcs.pms.handler.ProjectMemberDeleteHandler;
import com.eomcs.pms.handler.ProjectMemberUpdateHandler;
import com.eomcs.pms.handler.ProjectSearchHandler;
import com.eomcs.pms.handler.ProjectUpdateHandler;
import com.eomcs.pms.handler.TaskAddHandler;
import com.eomcs.pms.handler.TaskDeleteHandler;
import com.eomcs.pms.handler.TaskDetailHandler;
import com.eomcs.pms.handler.TaskListHandler;
import com.eomcs.pms.handler.TaskUpdateHandler;
import com.eomcs.util.Prompt;

public class ClientApp {

  // ???????????? ????????? ????????? ????????? ????????? ?????? ??????
  ArrayDeque<String> commandStack = new ArrayDeque<>();
  LinkedList<String> commandQueue = new LinkedList<>();

  String serverAddress;
  int port;

  public static void main(String[] args) {
    ClientApp app = new ClientApp("localhost", 8888);

    try {
      app.execute();

    } catch (Exception e) {
      System.out.println("??????????????? ?????? ??? ?????? ??????!");
      e.printStackTrace();
    }
  }

  public ClientApp(String serverAddress, int port) {
    this.serverAddress = serverAddress;
    this.port = port;
  }

  public void execute() throws Exception {

    // Mybatis ?????? ????????? ?????? ?????? ????????? ?????? ??????
    InputStream mybatisConfigStream = Resources.getResourceAsStream(
        "com/eomcs/pms/conf/mybatis-config.xml");

    // SqlSessionFactory ?????? ??????
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(mybatisConfigStream);

    // DAO??? ????????? SqlSession ?????? ??????
    // => ?????? commit ?????? ???????????? SqlSession ????????? ????????????.
    SqlSession sqlSession = sqlSessionFactory.openSession(false);

    // ???????????? ????????? DAO ?????? ??????
    BoardDao boardDao = new BoardDaoImpl(sqlSession);
    MemberDao memberDao = new MemberDaoImpl(sqlSession);
    ProjectDao projectDao = new ProjectDaoImpl(sqlSession);
    TaskDao taskDao = new TaskDaoImpl(sqlSession);

    // ????????? ????????? ???????????? ????????? ?????? ????????????.
    HashMap<String,Command> commandMap = new HashMap<>();

    commandMap.put("/board/add", new BoardAddHandler(boardDao));
    commandMap.put("/board/list", new BoardListHandler(boardDao));
    commandMap.put("/board/detail", new BoardDetailHandler(boardDao));
    commandMap.put("/board/update", new BoardUpdateHandler(boardDao));
    commandMap.put("/board/delete", new BoardDeleteHandler(boardDao));
    commandMap.put("/board/search", new BoardSearchHandler(boardDao));

    commandMap.put("/member/add", new MemberAddHandler(memberDao));
    commandMap.put("/member/list", new MemberListHandler(memberDao));
    commandMap.put("/member/detail", new MemberDetailHandler(memberDao));
    commandMap.put("/member/update", new MemberUpdateHandler(memberDao));
    commandMap.put("/member/delete", new MemberDeleteHandler(memberDao));

    MemberValidator memberValidator = new MemberValidator(memberDao);

    commandMap.put("/project/add", new ProjectAddHandler(projectDao, memberValidator));
    commandMap.put("/project/list", new ProjectListHandler(projectDao));
    commandMap.put("/project/detail", new ProjectDetailHandler(projectDao));
    commandMap.put("/project/update", new ProjectUpdateHandler(projectDao, memberValidator));
    commandMap.put("/project/delete", new ProjectDeleteHandler(projectDao, taskDao));
    commandMap.put("/project/search", new ProjectSearchHandler(projectDao));
    commandMap.put("/project/detailSearch", new ProjectDetailSearchHandler(projectDao));
    commandMap.put("/project/memberUpdate", new ProjectMemberUpdateHandler(projectDao, memberValidator));
    commandMap.put("/project/memberDelete", new ProjectMemberDeleteHandler(projectDao));

    commandMap.put("/task/add", new TaskAddHandler(taskDao, projectDao, memberValidator));
    commandMap.put("/task/list", new TaskListHandler(taskDao));
    commandMap.put("/task/detail", new TaskDetailHandler(taskDao));
    commandMap.put("/task/update", new TaskUpdateHandler(taskDao, projectDao, memberValidator));
    commandMap.put("/task/delete", new TaskDeleteHandler(taskDao));

    try {

      while (true) {

        String command = com.eomcs.util.Prompt.inputString("??????> ");

        if (command.length() == 0) {
          continue;
        }

        // ???????????? ????????? ????????? ???????????????.
        commandStack.push(command);
        commandQueue.offer(command);

        try {
          switch (command) {
            case "history":
              printCommandHistory(commandStack.iterator());
              break;
            case "history2":
              printCommandHistory(commandQueue.iterator());
              break;
            case "quit":
            case "exit":
              System.out.println("??????!");
              return;
            default:
              Command commandHandler = commandMap.get(command);

              if (commandHandler == null) {
                System.out.println("????????? ??? ?????? ???????????????.");
              } else {
                commandHandler.service();
              }
          }
        } catch (Exception e) {
          System.out.println("------------------------------------------");
          System.out.printf("????????? ?????? ??? ?????? ??????: %s\n", e.getMessage());
          System.out.println("------------------------------------------");
        }
        System.out.println(); // ?????? ????????? ????????? ???????????? ?????? ??? ??? ??????
      }

    } catch (Exception e) {
      System.out.println("????????? ?????? ?????? ?????? ?????? ??????!");
    }

    sqlSession.close();
    Prompt.close();
  }

  private void printCommandHistory(Iterator<String> iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      System.out.println(iterator.next());
      if ((++count % 5) == 0) {
        String input = Prompt.inputString(": ");
        if (input.equalsIgnoreCase("q")) {
          break;
        }
      }
    }
  }
}
