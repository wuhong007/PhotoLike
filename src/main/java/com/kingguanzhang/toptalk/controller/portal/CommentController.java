package com.kingguanzhang.toptalk.controller.portal;

import com.kingguanzhang.toptalk.dto.Msg;
import com.kingguanzhang.toptalk.entity.*;
import com.kingguanzhang.toptalk.service.*;
import org.hibernate.query.criteria.internal.expression.function.AggregationFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CommentController {

    @Autowired
    private CommentServiceImpl commentService;

    @Autowired
    private CRESTServiceImpl crestService;

    @Autowired
    private TopicServiceImpl topicService;
    @Autowired
    private EssayServiceImpl essayService;
    @Autowired
    private StoryServiceImpl storyService;
    @Autowired
    private PraiseServiceImpl praiseService;

    /**
     * ajax点赞操作;
     * @param request
     * @param commentId
     * @return
     */
    @RequestMapping(value = "/comment/json/praise",method = RequestMethod.POST)
    @ResponseBody
    private Msg praiseComment(HttpServletRequest request,@RequestParam("commentId")String commentId){
        /**
         * 判断session中是否有user,没有则返回到错误页面或重新登录界面;
         */
        if (null == request.getSession().getAttribute("user")){
            return Msg.fail().setCode(101).setMsg("用户未登录或登录已超时,请重新登录后再评论!");
        }

        /**
         * 判断是否是重复点赞;
         */
        User user = (User) request.getSession().getAttribute("user");
        Praise praise = new Praise();
        praise.setCommentId(Long.parseLong(commentId));
        praise.setUserId(user.getId());
        ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("topicId").withIgnorePaths("id").withIgnorePaths("essayId").withIgnorePaths("storyId").withIgnorePaths("eventId");
        Example<Praise> example = Example.of(praise,exampleMatcher);
        Pageable pageable = new PageRequest(0,2);//只需要查出一个就能判断是重复的;
        Page<Praise> allByExample = praiseService.findAllByExample(example, pageable);
        if (allByExample.hasContent()){
            return Msg.success().setMsg("您已经赞过了");
        }
        /**
         * 如果没有重复点赞,则保存此次操作;
         */
        try{
            praiseService.save(praise);
        }catch (Exception e){
            e.printStackTrace();
            return Msg.fail().setMsg("操作失败,请尝试刷新后重试");
        }

        /**
         * 查询Comment信息并将点赞数+1;
         */
        try {
            Comment comment = commentService.findById(Long.parseLong(commentId));
            long praiseNumber = comment.getPraiseNumber();
            comment.setPraiseNumber(praiseNumber + 1);
            commentService.save(comment);
        }catch (Exception e){
            return Msg.fail().setMsg("更新点赞总数时发生异常");
        }
        return Msg.success().setMsg("点赞成功");
    }

    /**
     * ajax取消点赞操作;
     * @param request
     * @param commentId
     * @return
     */
    @RequestMapping(value = "/comment/json/cancelPraise",method = RequestMethod.POST)
    @ResponseBody
    private Msg cancelPraiseComment(HttpServletRequest request,@RequestParam("commentId")String commentId){
        /**
         * 判断session中是否有user,没有则返回到错误页面或重新登录界面;
         */
        if (null == request.getSession().getAttribute("user")){
            return Msg.fail().setCode(101).setMsg("用户未登录或登录已超时,请重新登录后再评论!");
        }

        /**
         * 删除数据库中的点赞记录;
         */
        User user = (User) request.getSession().getAttribute("user");
        try {
            praiseService.deletePraiseComment(user.getId(),Long.parseLong(commentId));
        }catch (Exception e){
            return Msg.fail().setMsg("操作失败,刷新后再重试");
        }

        /**
         * 查询Comment信息并将点赞数-1;
         */
        try {
            Comment comment = commentService.findById(Long.parseLong(commentId));
            long praiseNumber = comment.getPraiseNumber();
            comment.setPraiseNumber(praiseNumber -1);
            commentService.save(comment);
        }catch (Exception e){
            return Msg.fail().setMsg("更新点赞总数时发生异常");
        }
        return Msg.success().setMsg("取消点赞成功");
    }


    /**
     * ajax回复父评论;
     * @param request
     * @return
     */
    @RequestMapping(value = "/comment/json/subcomment/add",method = RequestMethod.POST)
    @ResponseBody
    private Msg subcommentAdd(HttpServletRequest request, @RequestParam("subcomment")String subcomment,@RequestParam("supcommentId")String supcommentId,@RequestParam("plateAndId")String plateAndId){
        /**
         * 判断session中是否有user,没有则返回到错误页面或重新登录界面;
         */
        if (null == request.getSession().getAttribute("user")){
            return Msg.fail().setCode(101).setMsg("用户未登录或登录已超时,请重新登录后再评论!");
        }
        /**
         * 初始化Comment信息;
         */
        User user = (User) request.getSession().getAttribute("user");
        Comment comment1 = new Comment();
        comment1.setAuthor(user);
        comment1.setContent(subcomment);
        comment1.setCreatTime(new Date(System.currentTimeMillis()));
        comment1.setSupcommentId(Long.parseLong(supcommentId));
        //保存Comment
        try{
           commentService.save(comment1);
        }catch (Exception e){
            return Msg.fail().setMsg("保存评论失败");
        }

        /**
         * 判断操作的页面板块是专辑还是故事,将其评论数+1,前端传过来的模板是"板块名.id",例如"topic.1";
         */
        String plateName = plateAndId.substring(0, plateAndId.indexOf("."));
        String id = plateAndId.substring(plateAndId.indexOf(".")+1);
        switch (plateName){
            case "topic":
                Topic topic = topicService.findById(Long.parseLong(id));
                long topicCommentNumber = topic.getCommentNumber();
                topic.setCommentNumber(topicCommentNumber+1);
                try {
                    topicService.save(topic);
                }catch (Exception e){
                    return Msg.fail().setMsg("更新评论总数时出现异常!");
                }
                break;
            case "story":
                Story story = storyService.findById(Long.parseLong(id));
                long storyCommentNumber = story.getCommentNumber();
                story.setCommentNumber(storyCommentNumber+1);
                try{
                    storyService.save(story);
                }catch (Exception e){
                    return Msg.fail().setMsg("更新评论总数时出现异常!");
                }
                break;
        }
        return Msg.success().setMsg("评论成功!");

    }

    /**
     * ajax发布评论;
     * @param request
     * @param commentStr
     * @return
     */
    @RequestMapping(value = "/comment/json/add",method = RequestMethod.POST)
    @ResponseBody
    private Msg commentAdd(HttpServletRequest request, @RequestParam("comment")String commentStr,@RequestParam("plateAndId")String plateAndId){
        /**
         * 判断session中是否有user,没有则返回到错误页面或重新登录界面;
         */
        if (null == request.getSession().getAttribute("user")){
             return Msg.fail().setCode(101).setMsg("用户未登录或登录已超时,请重新登录后再评论!");
        }
        /**
         * 初始化Comment信息;
         */
        User user = (User) request.getSession().getAttribute("user");
        Comment comment1 = new Comment();
        comment1.setAuthor(user);
        comment1.setContent(commentStr);
        comment1.setCreatTime(new Date(System.currentTimeMillis()));
        //保存Comment并返回id;
        long commentId;
        try{
            commentId = commentService.saveAndFlush(comment1);
        }catch (Exception e){
            return Msg.fail().setMsg("保存评论失败");
        }

        /**
         * 判断操作的页面板块是专辑还是故事,设置关联并将其评论数+1,前端传过来的模板是"板块名.id",例如"topic.1";
         */
        String plateName = plateAndId.substring(0, plateAndId.indexOf("."));
        String id = plateAndId.substring(plateAndId.indexOf(".")+1);
        /**
         * 初始化保存关联信息的实体类;
         */
        CommentRelateEST commentRelateEST = new CommentRelateEST();
        commentRelateEST.setCommentId(commentId);
        switch (plateName){
            case "topic":
                commentRelateEST.setTopicId(Long.parseLong(id));//设置关联
                Topic topic = topicService.findById(Long.parseLong(id));
                long topicCommentNumber = topic.getCommentNumber();
                topic.setCommentNumber(topicCommentNumber+1);//评论总数+1
                try {
                    topicService.save(topic);
                }catch (Exception e){
                    return Msg.fail().setMsg("更新评论总数时出现异常!");
                }
                break;
            case "story":
                commentRelateEST.setStoryId(Long.parseLong(id));//设置关联
                Story story = storyService.findById(Long.parseLong(id));
                long storyCommentNumber = story.getCommentNumber();
                story.setCommentNumber(storyCommentNumber+1);//评论总数+1
                try{
                    storyService.save(story);
                }catch (Exception e){
                    return Msg.fail().setMsg("更新评论总数时出现异常!");
                }
                break;
        }
        crestService.save(commentRelateEST);//保存关联关系实体类;

        return Msg.success().setMsg("评论成功!");

    }


    /**
     * ajax获取父评论下的子评论
     * @param supcommentIds
     * @param pn
     * @return
     */
    @RequestMapping(value = "/comment/json/subcomments",method = RequestMethod.POST)
    @ResponseBody
    public Msg getSubComment(HttpServletRequest request,@RequestParam("supcommentIds")String supcommentIds, @RequestParam(value = "pn",defaultValue = "1")Integer pn){

        /**
         * 处理字符串参数
         */
        if (null == supcommentIds || "" == supcommentIds.trim()){
            return Msg.fail().setMsg("没有收到任何父评论id参数");
        }
        String[] split = supcommentIds.split(",");
        //定义一个map用于存放对应的subcommentPage, 键为父评论id,值为子评论的page;
        Map<Long,Page<Comment>> subcommentPageMap = new HashMap<>();

        Comment comment =new Comment();
        ExampleMatcher exampleMatcher;
        Example<Comment> example;
        Pageable pageable;
        Page<Comment> subCommentPage;

        /**
         * 初始化查询点赞的模板,返回一个记录当前页被收藏的评论Id的拼接字符串
         */
        String praiseCommentIds = "";
        User user ;
        Praise praise = new Praise();
        if (null != request.getSession().getAttribute("user")){
            user = (User) request.getSession().getAttribute("user");
            praise.setUserId(user.getId());
        }


        //开始遍历,每遍历出一个id就查一次数据库
        for (String supcommentId:split){
            long id = Long.parseLong(supcommentId);
            /**
             * 创建查询对象并设置父类id
             */
            comment.setSupcommentId(id);
            //创建匹配器,设置匹配规则
            exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id").withIgnorePaths("praiseNumber");//注意需要忽略id,因为id是long类型,默认值不是null而是0;忽略掉点赞状态;
            //创建example对象,传入查询对象和匹配器
            example= Example.of(comment,exampleMatcher);

            pageable= new PageRequest(0,999999,new Sort(Sort.Direction.DESC,"id"));
            subCommentPage = commentService.findAllByExample(example, pageable);

            /**
             * 如果没有查出数据则不往map里存,这样前台直接遍历map即可;
             */
            if (null != subCommentPage && subCommentPage.hasContent()){
                subcommentPageMap.put(id,subCommentPage);
                /**
                 * 如果查出,则继续查子评论是否被当前用户点赞;
                 */
                for (Comment subcomment:subCommentPage){
                    praise.setCommentId(subcomment.getId());
                    ExampleMatcher exampleMatcher2 = ExampleMatcher.matching().withIgnorePaths("id").withIgnorePaths("topicId").withIgnorePaths("storyId").withIgnorePaths("essayId").withIgnorePaths("eventId");
                    Example<Praise> example2 = Example.of(praise,exampleMatcher2);
                    Pageable pageable1 = new PageRequest(0,2);
                    if (praiseService.findAllByExample(example2,pageable1).hasContent()){
                        praiseCommentIds = praiseCommentIds+subcomment.getId()+ ",";
                    }
                }

            }



        }
        if (null == subcommentPageMap){
            return Msg.fail().setMsg("没有查询到任何子评论");
        }
        if ("" != praiseCommentIds){
            praiseCommentIds = praiseCommentIds.substring(0,praiseCommentIds.lastIndexOf(","));
        }

        return Msg.success().add("subcommentPageMap",subcommentPageMap).add("praiseCommentIds",praiseCommentIds);

    }
}
