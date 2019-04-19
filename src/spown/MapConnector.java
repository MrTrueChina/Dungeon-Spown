/**
 * 房间和迷宫的连接
 * 
 * 每个房间都需要能走通
 * 
 * 0.迷宫不保证连续
 *  1.
 * 1.房间可能被房间包围导致无法连接迷宫
 * 2.所有房间连接到迷宫或已经连接到迷宫的房间
 * 
 * 1.寻找一个房间作为起点，房间里的所有地块加入已经连接表，开门
 * 2.
 * 3.
 * 
 * 迷宫不可能紧靠，否则生成时就会生成过去。如果迷宫分为多个部分则必然是被房间分隔，因此所有可以开门的位置只有房间与房间和房间到迷宫。也就是说所有开门位置都可以以房间为起点进行搜索
 * 生成时在连接房间和迷宫时没有连接到的位置相当于不可见，这使得必须要有某种提示信息存在，否则只能进行高成本的遍历
 * 可以在房间外围制造生成点，但问题是有的房间距离迷宫和其他房间有厚度为2的墙，这使得生成点具有方向
 * 更大的问题是两个房间之间的生成点也有方向性，此时生成点如何表现出两个方向性？
 * 或者生成点没有方向性而是在打通的时候有方向性？
 * 
 * 连接点是由房间的四条边向外直线延伸，直到找到空地。因此和连接点相邻的地块必然在房间的正上下或正左右方向，不会有斜着的情况。
 * 
 * 
 * 连接连接前很可能需要把所有房间、迷宫都保存起来，之后要有一个合并方法，把连接起来的房间和迷宫合并到一个叫“主区域”的表里。
 * 
 * a.判断一个连接点是不是可以移除的方法应该是（一个连接点上下左右四个方向，长度最大为2，除了生成点外最近的地块，是不是墙或主区域。如果不是墙也不是主区域，则说明这个生成点能连接到没连接到的位置。）
 * 
 * b.移除连接点的第二种思路：以房间为单位移除连接点。因为迷宫之间不可能有连接点，那么所有连接点肯定都能从房间为起点找到（可以直接设计为连接点从房间向外生成）
z*   既然连接点都可以通过房间找到，那就方便多了：
z*   在连接一个房间之后，遍历这个房间的所有生成点，从房间内向外走，看最近的空地是不是主区域的空地，是的话就说明走过的所有生成点都没用了，不是的话说明生成点指向没有连接的区域
 *   
 *   
z* 生成点部分：
 * 
z*  遍历房间
 *  {
z*      遍历房间上边
 *      {
z*          遍历每个地块
 *          {
z*              递归或循环向上查找地块 (找3格)
 *              {
 *                  if (到达了地图边缘)
 *                      return
 *              
 *                  if (找到一个是空地的地块) 【此处有扩展性问题】假设有个房间是内凹的，房间将可能自己和自己创建连接点，可以通过判断是不是房间自己的方块来解决
 *                  {
z*                      一路上所有墙创建连接点
 *                      return
 *                  }
 *              }
 *          }
 *      }
 *      
z*      遍历其他三边，类似于左边的处理
 *  }
 *  
 *  
z*  连接部分：
 *  
z*  随机选一个房间，所有地块加入主区域表
 *  
 *  while(还有生成点)
 *  {
 *      List 相邻的生成点 = 遍历主区域地块，获取所有和主区域相邻的生成点
 *      
z*      从相邻的生成点里随机一个生成点
z*      在这个生成点上下左右四个方向里找到主区域的地块 -> 这个地块到生成点的方向就是相邻区域的方向
z*      沿着这个方向找到下一个空地
z*      把中间这段墙打通
z*      把下一个区域所有方块加入到主区域
 *      
 *      if(主区域方向的地块属于某个房间)
z*          移除房间生成点(主区域房间)
 *      if(相邻区域的地块属于某个房间)
z*          移除房间生成点(相邻区域房间)
 *  }
 *  
 *  void 移除房间连接点(Room 房间)
 *  {
z*      遍历房间上边
 *      {
 *          if(地块的上边一格是连接点)
 *          {
z*              向上找到下一个空地
 *              if(这个空地在主区域里)
 *              {
z*                  移除经过的所有格子里的连接点
 *              }
 *          }
 *      }
 *      
z*      遍历其他三个边
 *  }
 */

package spown;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import map.Map;
import maze.Maze;
import quad.Quad;
import quad.QuadType;
import random.Random;
import room.Room;
import vector.Vector;

public class MapConnector {
    private Map _map;
    private ArrayList<Room> _rooms;
    private ArrayList<Maze> _mazes;
    private MapSpownData _spownData;
    private boolean[][] _connectPoints;
    private HashSet<Quad> _mainZoneQuads; // 利用 HashSet 的无序性来进行随机查找与主区域相邻的连接点

    /**
     * 连接房间和迷宫
     * 
     * @param map
     * @param rooms
     */
    public Map ConnectRoomsAndMaze(Map map, ArrayList<Room> rooms, ArrayList<Maze> mazes, MapSpownData spownData) {
        try {
            setupConnector(map, rooms, mazes, spownData);
            spownConnectPoints();
            StartConnect();
            return _map;
        } finally {
            clearConnector();
        }
    }

    private void setupConnector(Map map, ArrayList<Room> rooms, ArrayList<Maze> mazes, MapSpownData spownData) {
        _map = map;
        _rooms = rooms;
        _mazes = mazes;
        _spownData = spownData;
        _connectPoints = new boolean[_map.width][_map.height];
        _mainZoneQuads = new HashSet<Quad>();

        for (int y = 0; y < _map.height; y++)
            for (int x = 0; x < _map.width; x++)
                _connectPoints[x][y] = false;
    }

    private void clearConnector() {
        _map = null;
        _rooms = null;
        _spownData = null;
        _connectPoints = null;
        _mainZoneQuads = null;
    }

    /**
     * 生成连接点
     */
    private void spownConnectPoints() {
        /*
        z* 生成点部分：
         * 
        z *  遍历房间
         *  {
        z *      遍历房间上边每个地块
         *      {
        z*          递归或循环向上查找地块 (找3格)
         *          {
         *              if (到达了地图边缘)
         *                  return
         *              
         *              if (找到一个是空地的地块) 【此处有扩展性问题】假设有个房间是内凹的，房间将可能自己和自己创建连接点，可以通过判断是不是房间自己的方块来解决
         *              {
        z*                  一路上所有墙创建连接点
         *                  return
         *              }
         *          }
         *      }
         *      
        z*      遍历其他三边，类似于左边的处理
         *  }
         */
        System.out.println("开始生成连接点");
        for (Room room : _rooms)
            spownARoomConnectPoints(room);
    }

    private void spownARoomConnectPoints(Room room) {
        /**
        z      * 遍历房间上边每个地块
         *  {
        z        *      递归或循环向上查找地块 (找3格)
         *      {
         *          if (到达了地图边缘)
         *          return
         *              
         *          if (找到一个是空地的地块) 【此处有扩展性问题】假设有个房间是内凹的，房间将可能自己和自己创建连接点，可以通过判断是不是房间自己的方块来解决
         *          {
        z *              一路上所有墙创建连接点
         *              return
         *          }
         *      }
         *  }
         */
        //        System.out.println("生成房间 " + room + " 的连接点");
        for (Quad quad : room.getTopQuads())
            spownAQuadConnectPoints(quad, Vector.UP);
        for (Quad quad : room.getRightQuads())
            spownAQuadConnectPoints(quad, Vector.RIGHT);
        for (Quad quad : room.getBottomQuads())
            spownAQuadConnectPoints(quad, Vector.DOWN);
        for (Quad quad : room.getLeftQuads())
            spownAQuadConnectPoints(quad, Vector.LEFT);
    }

    private void spownAQuadConnectPoints(Point quadPosition, Vector direction) {
        /**
         *  递归或循环向上查找地块 (找3格)
         *  {
         *      if (到达了地图边缘)
         *      return
         *              
         *      if (找到一个是空地的地块) 【此处有扩展性问题】假设有个房间是内凹的，房间将可能自己和自己创建连接点，可以通过判断是不是房间自己的方块来解决
         *      {
         *          一路上所有墙创建连接点
         *          return
         *      }
         *  }
         */
        //        System.out.println("在 " + quadPosition + " 位置向 " + direction + " 方向生成连接点");

        spownAQuadConnectPoints(quadPosition, direction, 1); // 传 1 是因为 0 是房间边缘的地块，属于房间

        //测试部分
        int num = 0;
        for (int y = 0; y < _connectPoints[0].length; y++)
            for (int x = 0; x < _connectPoints.length; x++)
                if (_connectPoints[x][y])
                    num++;
        System.out.println("生成了 " + num + " 个连接点");
    }

    private boolean spownAQuadConnectPoints(Point quadPPosition, Vector direction, int step) {
        if (step > 3) // 根据算法，房间到最近的房间或迷宫的距离不会超过两个墙，那么当长度延伸到3的时候，要么已经遇到了空地，要么就没有走到正确的路线上
            return false;

        Point checkPoint = new Point(quadPPosition.x + direction.x * step, quadPPosition.y + direction.y * step);
        if (!_map.contains(checkPoint))
            return false;

        if (_map.getType(checkPoint) == QuadType.FLOOR)
            return true;

        if (spownAQuadConnectPoints(quadPPosition, direction, step + 1)) /*如果下一步能遇到空地*/ {
            addConnectPoint(quadPPosition.x + direction.x * step, quadPPosition.y + direction.y * step); // 则在这一个位置就应该是墙了，加入连接点
            return true;
        }

        return false;
    }

    private void addConnectPoint(int x, int y) {
        _connectPoints[x][y] = true;
    }

    /**
     * 开始连接
     */
    private void StartConnect() {
        /*
        *  连接部分：
        *  
        *  随机选一个房间，所有地块加入主区域表
        *  
        *  while(还有生成点)
        *  {
        *      List 相邻的生成点 = 遍历主区域地块，获取所有和主区域相邻的生成点
        *      
        *      从相邻的生成点里随机一个生成点
        *      在这个生成点上下左右四个方向里找到主区域的地块 -> 这个地块到生成点的方向就是相邻区域的方向
        *      沿着这个方向找到下一个空地
        *      把中间这段墙打通
        *      把下一个区域所有方块加入到主区域
        *      
        *      if(主区域方向的地块属于某个房间)
        *          移除房间生成点(主区域房间)
        *      if(相邻区域的地块属于某个房间)
        *          移除房间生成点(相邻区域房间)
        *  }
        *  
        *  void 连接一个房间 (Room 房间)
        *  {
        *      
        *  }
        *  
        *  void 移除房间连接点 (Room 房间)
        *  {
        *      遍历房间上边
        *      {
        *          if(地块的上边一格是连接点)
        *          {
        *              向上找到下一个不是连接点的地块
        *              if(这个地块在主区域里 || 这个地块是墙)
        *              {
        *                  移除经过的所有格子里的连接点
        *              }
        *          }
        *      }
        *      
        *      遍历其他三个边
        *  }
        */
        /*
            丁丁丁丁丁丁丁丁丁丁丁丁
            墙墙丁墙丁墙墙墙墙墙墙丁
            墙墙点点点点口口口口墙丁
            墙墙点点点点口口口口墙丁
            墙墙口口口点点点点点墙丁
            墙墙口口口点点点点点墙丁
        */
        connectStartRoom(); // 随机选一个房间，所有地块加入主区域表

        Point connectPoint = null;
        while ((connectPoint = getRandomConnectPoint()) != null)
            connectAConnectPoint(connectPoint);
    }

    /**
     * 连接第一个房间
     */
    private void connectStartRoom() {
        /*
         *  Room 房间 =  随机出一个房间
         *  房间中的地块加入到主区域
         *  
         *  还需要其他操作吗？似乎不需要，似乎没有清理生成点的必要性
         */
        Room room = getRandomRoom();
        addARoomQuadsToMainZone(room);
    }

    /**
     * 将一个房间的地块加入到主区域
     * 
     * @param room
     */
    private void addARoomQuadsToMainZone(Room room) {
        _mainZoneQuads.addAll(Arrays.asList(room.getQuadArray()));
    }

    private boolean haveConnectPoint() {
        for (int x = 0; x < _map.width; x++)
            for (int y = 0; y < _map.height; y++)
                if (_connectPoints[x][y])
                    return true;
        return false;
    }

    /**
     * 获取随机与主区域相连的连接点
     * 
     * @return
     */
    private Point getRandomConnectPoint() {
        /*
         *  1、遍历所有主区域地块（利用 HashSet 的无序性实现随机）
         *  {
         *      2、遍历当前地块的上下左右四个Point
         *      {
         *          if (找到生成点了)
         *              return 找到的生成点
         *      }
         *  }
         *  
         *  3、遍历完了也没找到，return null
         */
        Point connectPoint = null;
        for (Quad quad : _mainZoneQuads) {
            connectPoint = getContiguousConnectPoint(quad);
            if (connectPoint != null)
                break;
        }
        return connectPoint;
    }

    /**
     * 获取一个地块上下左右四个相邻位置中第一个是生成点的那个，如果四个都不是生成点则返回 null
     * 
     * @param quad
     * @return
     */
    private Point getContiguousConnectPoint(final Quad quad) {
        if (_connectPoints[quad.x][quad.y + 1])
            return new Point(quad.x, quad.y + 1);
        if (_connectPoints[quad.x + 1][quad.y])
            return new Point(quad.x + 1, quad.y);
        if (_connectPoints[quad.x][quad.y - 1])
            return new Point(quad.x, quad.y - 1);
        if (_connectPoints[quad.x - 1][quad.y])
            return new Point(quad.x - 1, quad.y);
        return null;
    }

    /**
     * 从一个连接点出发，连接连接点连接到的区域
     * 
     * @param connectPoint
     */
    private void connectAConnectPoint(Point connectPoint) {
        /*
         *  在这个生成点上下左右四个方向里找到主区域的地块 -> 这个地块到生成点的方向就是相邻区域的方向
         *  沿着这个方向找到下一个空地
         *  把中间这段墙打通
         *  把下一个区域所有方块加入到主区域
         */
        Point mainPoint = getContiguousMainQuad(connectPoint);
        Vector direction = new Vector(connectPoint.x - mainPoint.x, connectPoint.y - mainPoint.y);

        for (int step = 1;; step++) {
            Point currentPoint = new Point(mainPoint.x + direction.x * step, mainPoint.y + direction.y * step);
            _map.setType(currentPoint, QuadType.FLOOR);
            _connectPoints[currentPoint.x][currentPoint.y] = false;

            Quad currentQuad = _map.getQuad(currentPoint.x, currentPoint.y);
            if (currentQuad.getType() == QuadType.FLOOR) /*找到地板了*/ {
                Room currentRoom = getRoome(currentQuad);
                if (currentRoom != null) {
                    connectARoom(currentRoom);

                    //if(getRoome(_map.getQuad(mainPoint.x,mainPoint.y)) != null)
                    //    clearARoomConnectPoint(getRoome(_map.getQuad(mainPoint.x,mainPoint.y)));
                    //房间连房间的情况，新房间清理连接点会把老房间的一起清理掉，不用特别处理老房间。但留下这些有可能能够和开迷宫合并重用
                    return;
                }

                Maze currentMaze = getMaze(currentQuad);
                if (currentMaze != null) {
                    connectAMaze(currentMaze);

                    if (getRoome(_map.getQuad(mainPoint.x, mainPoint.y)) != null)
                        clearARoomConnectPoint(getRoome(_map.getQuad(mainPoint.x, mainPoint.y))); // 清除起步房间的连接点

                    return;
                }
            }
        }
    }

    private Point getContiguousMainQuad(Point center) {
        if (_mainZoneQuads.contains(_map.getQuad(center.x, center.y + 1)))
            return _map.getQuad(center.x, center.y + 1);
        if (_mainZoneQuads.contains(_map.getQuad(center.x + 1, center.y)))
            return _map.getQuad(center.x + 1, center.y);
        if (_mainZoneQuads.contains(_map.getQuad(center.x, center.y - 1)))
            return _map.getQuad(center.x, center.y - 1);
        if (_mainZoneQuads.contains(_map.getQuad(center.x - 1, center.y)))
            return _map.getQuad(center.x - 1, center.y);
        return null;
    }

    private boolean isNewZone(Quad quad) {
        for (Room room : _rooms)
            if (room.contains(quad))
                return false;

        for (Maze maze : _mazes)
            if (maze.contains(quad))
                return false;

        return true;
    }

    /**
     * 获取地块所属的房间
     * 
     * @param quad
     * @return 地块所属的房间，如果地块不属于任何房间则返回 null
     */
    private Room getRoome(Quad quad) {
        for (Room room : _rooms)
            if (room.contains(quad))
                return room;
        return null;
    }

    /**
     * 获取地块所属的迷宫
     * 
     * @param quad
     * @return 地块所属的迷宫，如果地块不属于任何迷宫则返回 null
     */
    private Maze getMaze(Quad quad) {
        for (Maze maze : _mazes)
            if (maze.contains(quad))
                return maze;
        return null;
    }

    private Room getRandomRoom() {
        return _rooms.get(Random.Range(0, _rooms.size()));
    }

    /**
     * 将房间加入主区域
     * 
     * @param room
     */
    private void connectARoom(Room room) {
        /*
         *  将这个房间所有的地块加入到主区域
         *  清除这个房间的连接点
         */
        addARoomQuadsToMainZone(room);
        clearARoomConnectPoint(room);
    }

    /**
     * 清理房间的连接点
     * 
     * @param room
     */
    private void clearARoomConnectPoint(Room room) {
        /*
         *  遍历上下左右四个边
         *  {
         *      遍历边上的所有地块
         *      {
         *          清除这个地块所指向的连接点
         *      }
         *  }
         */
        for (Quad quad : room.getTopQuads())
            clearAQuadConnectPoint(quad, Vector.UP);
        for (Quad quad : room.getRightQuads())
            clearAQuadConnectPoint(quad, Vector.RIGHT);
        for (Quad quad : room.getBottomQuads())
            clearAQuadConnectPoint(quad, Vector.DOWN);
        for (Quad quad : room.getLeftQuads())
            clearAQuadConnectPoint(quad, Vector.LEFT);
    }

    /**
     * 清除一个地块的连接点
     * 
     * @param startPoint
     * @param direction 这个地块的连接点相对于地块的方向
     */
    private void clearAQuadConnectPoint(Point startPoint, Vector direction) {
        clearConnectPoint(startPoint, direction, 1); // 从第一步开始，第零步是房间边缘的地块，肯定不是连接点
    }

    private boolean clearConnectPoint(Point startPoint, Vector direction, int step) {
        /*
         *  设计上如果方向正确连接点最少没有最多两个
         *      0个：此路不通，应直接结束
         *      1个：前进第二步后会到达不是墙的地块
         *      2个：前进第三步后会到达不是墙的地块
         *  得出结论：
         *      1.最多走三步
         *      2.遇到不是连接点的位置应该停止
         *      3.停止的地方如果是墙，则说明此路不通
         *      4.停止的地方如果是空地，则说明这条路可以走通
         */
        /*
         *  以 true 作为可以移除，以 false 作为不可移除
         * 
         *  if (到达的位置不是连接点) -> 2.不是连接点的位置停止
         *  {
         *      if (到达的位置是墙)
         *          此路不通 return false -> 3.停止的地方是墙，此路不通
         *          
         *      if (到达的位置是空地)
         *          走对了 return true -> 4.停止的地方是空地，可以走通
         *  }
         *  
         *  if (向下递归是 true)
         *  {
         *      移除自己这一格的生成点
         *      return true
         *  }
         *  else
         *  {
         *      return false
         *  }
         * 
         *  if (步数 > 3) -> 1.最多走三步
         *      逻辑上发生严重错误 return false
         *  
         *  return false -> 默认是 false
         */
        Point currentPoint = new Point(startPoint.x + direction.x * step, startPoint.y + direction.y * step);
        if (!_connectPoints[currentPoint.x][currentPoint.y]) {
            if (_map.getType(currentPoint) == QuadType.FLOOR)
                return true;

            if (_map.getType(currentPoint) == QuadType.WALL)
                return false;
        }

        if (clearConnectPoint(startPoint, direction, step + 1)) {
            _connectPoints[currentPoint.x][currentPoint.y] = false;
            return true;
        }

        return false;

        //        if (step > 3)
        //            return false;
        //
        //        Point checkPoint = new Point(startPoint.x + direction.x * step, startPoint.y + direction.y * step);
        //        if (!_map.contains(checkPoint))
        //            return false;
        //
        //        return false;
    }

    /**
     * 讲一个迷宫归入主区域
     * 
     * @param maze
     */
    private void connectAMaze(Maze maze) {
        /*
         *  将迷宫所有地块加入主区域
         *  
         *  还有其他操作吗？似乎没有了
         */
        System.out.println(maze.getQuads().length);
        _mainZoneQuads.addAll(Arrays.asList(maze.getQuads()));
    }

    private int getDoorNamber() {
        int[] probability = _spownData.roomDoorsProbability;

        if (probability == null || probability.length == 0)
            return 1;

        int totalProbability = 0;
        for (int i : probability) {
            totalProbability += i;
        }
        if (totalProbability <= 0)
            return 1;

        int probabilityValue = Random.Range(0, totalProbability);
        for (int i = 0; i < probability.length; i++) {
            probabilityValue -= i;
            if (probabilityValue <= 0)
                return i + 1;
        }

        return 1;
    }

    private boolean ConnectADoor(Room room) {
        /**
         * 0.保证能走到迷宫
         *   1.迷宫不一定连续
         *   2.房间不一定连接到迷宫
         * 1.确定方向
         * 2.选取位置
         * 3.向前探路
         */
        return false;
    }

}