package com.kingguanzhang.toptalk.controller.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingguanzhang.toptalk.dto.Msg;
import com.kingguanzhang.toptalk.entity.*;
import com.kingguanzhang.toptalk.service.*;
import com.kingguanzhang.toptalk.utils.DownloadZip;
import com.kingguanzhang.toptalk.utils.ImgUtil;
import com.kingguanzhang.toptalk.utils.PathUtil;
import com.kingguanzhang.toptalk.utils.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
public class TopicController {


    @Autowired
    private CategoryServiceImpl categoryService;
    @Autowired
    private TopicServiceImpl topicService;
    @Autowired
    private CommentServiceImpl commentService;
    @Autowired
    private UserFavoriteServiceImpl userFavoriteService;
    @Autowired
    private PraiseServiceImpl praiseService;



    /**
     * topic详情
     * @param model
     * @param id
     * @return
     */
    @RequestMapping("/topic/{topicId}")
    public String toTopicPage(HttpServletRequest request,Model model,  @RequestParam(value = "commentSort",defaultValue = "new")String commentSort,@PathVariable("topicId")String id,@RequestParam(value = "pn",defaultValue = "1")Integer pn){

        /**
         * 判断收藏状态,返回一个名为favStatus 的布尔值给页面;
         
         */
        Boolean favStrtus = false;
        if (null != request.getSession().getAttribute("user")){
            User user = (User) request.getSession().getAttribute("user");
            UserFavorite userFavorite = new UserFavorite();
            userFavorite.setUserId(user.getId());
            userFavorite.setTopicId(Long.parseLong(id));
            ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id").withIgnorePaths("essayId").withIgnorePaths("storyId");//所有Long 类型的属性默认值都是0不是null,所以要忽略;
            Example<UserFavorite> example = Example.of(userFavorite,exampleMatcher);
            Pageable pageable = new PageRequest(0,2);
            //查询是否收藏了;
            if (userFavoriteService.findAllByExample(example, pageable).hasContent()){//不使用findOne,因为当数据库有重复时findOne会抛异常;
                favStrtus = true;
            }
        }
        model.addAttribute("favStatus",favStrtus);



        /**
         * 获取指定id的topic;这里需要处理一下内容图片的地址字符串;
         */
        Topic topic = topicService.findById(Long.parseLong(id));
        if (null == topic){
            return "error";// TODO 需要完成错误页面 提示此稿件未找到:
        }

        if (null != topic.getContentImgsAddr()){
            String contentImgsAddr = topic.getContentImgsAddr();
            String[] imgAddrArry = contentImgsAddr.split(",");
            List<String> imgAddrList = new ArrayList<>();
            for (String imgAddr:imgAddrArry){
                imgAddrList.add(imgAddr);
            }
            topic.setImgAddrList(imgAddrList);
        }
        /**
         * 限制浏览者只能浏览状态为1的topic,除非浏览者是作者或管理员
         */
        if(1 == topic.getStatus() || null != request.getSession().getAttribute(("admin"))){
            model.addAttribute("topic",topic);
        }else if (null != request.getSession().getAttribute("user")){
            User user = (User) request.getSession().getAttribute("user");
            if (user.getId() == topic.getAuthor().getId()){
                model.addAttribute("topic",topic);
            }
        }else {
            return "error"; // TODO 需要完成错误页面 提示没有权限访问此稿件:
        }


        /**
         * 获取热专栏,只显示5个;
         */
        Pageable pageable3 = new PageRequest(0,5,  new Sort(Sort.Direction.DESC,"collectNumber"));
        Topic hotTopic = new Topic();
        hotTopic.setStatus(1);//查出通过审核的状态为展示的专辑;
        ExampleMatcher exampleMatcher3 = ExampleMatcher.matching().withIgnorePaths("id","collectNumber","commentNumber");//long类型的需要忽略;
        Example<Topic> example3 = Example.of(hotTopic,exampleMatcher3);
        Page<Topic> hotTopicPage = topicService.findAllByExample(example3,pageable3);
        model.addAttribute("hotTopicPage",hotTopicPage);

        /** 新方案中规范每个topic只对应一个分类,所以可以通过级联查询出分类;
         * 获取topic关联的category,只显示4个即可
        Pageable pageable4 = new PageRequest(0,4,  new Sort(Sort.Direction.DESC,"id"));
        Page<Category> categoryPage = categoryService.findByTopicId(Long.parseLong(id), pageable4);
        model.addAttribute("categoryPage",categoryPage);*/

        /**
         * 获取topic关联的父Comment,sql语句中已经排除了评论表中supcomment_id 不等于0的情况(即排除掉此评论为子评论时的情况);
         * 请求参数中的commentSort对应的值代表评论排序规则,new代表按最新排序,hot代表按最热排序(点赞数);
         */
        Pageable pageable5 ;
        if ("new" == commentSort) {
             pageable5 = new PageRequest(pn - 1, 10, new Sort(Sort.Direction.DESC, "creat_time"));
        }else {
             pageable5 = new PageRequest(pn - 1, 10, new Sort(Sort.Direction.DESC, "praise_number"));
        }
        Page<Comment> commentPage = commentService.findByTopicId(Long.parseLong(id), pageable5);
        //同时将排序状态返回,方便页面渲染翻页链接:
        model.addAttribute("commentSort",commentSort);
        model.addAttribute("commentPage",commentPage);

        /**
         * 判断当前取出的评论是否被用户点赞,返回一个记录当前页被收藏的评论Id的拼接字符串
         */
        String praiseCommentIds = "";
        if (null != request.getSession().getAttribute("user")){
            User user = (User) request.getSession().getAttribute("user");
            Praise praise = new Praise();
            praise.setUserId(user.getId());
            for(Comment temp:commentPage.getContent()){
                praise.setCommentId(temp.getId());
                ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id").withIgnorePaths("topicId").withIgnorePaths("storyId").withIgnorePaths("essayId").withIgnorePaths("eventId");
                Example<Praise> example = Example.of(praise,exampleMatcher);
                Pageable pageable1 = new PageRequest(0,2);
                if (praiseService.findAllByExample(example,pageable1).hasContent()){
                    praiseCommentIds = praiseCommentIds+temp.getId() + ",";
                }
            }
        }
        if ("" != praiseCommentIds){
            praiseCommentIds = praiseCommentIds.substring(0,praiseCommentIds.lastIndexOf(","));
        }
        model.addAttribute("praiseCommentIds",praiseCommentIds);

        return "portal/topicDetails";
    }




    /**
     * 点击分类后获取分类下的topic并分页排序;
     * @param model
     * @param request
     * @param pn
     * @return
     */
    @RequestMapping("/topic")
    public String toTopicPageByCategory(Model model, HttpServletRequest request, @RequestParam(name = "pn",defaultValue = "1") Integer pn){

        /**
         * 获取所有的分类显示在页面上方的分页导航;
         */
        Pageable pageable = new PageRequest(0,100,  new Sort(Sort.Direction.DESC,"id"));
        Page<Category> categoryPage = categoryService.findAll(pageable);
        model.addAttribute("categoryPage",categoryPage);


        /**
         * 获取热门专栏
         */
        Pageable pageable3 = new PageRequest(0,5,  new Sort(Sort.Direction.DESC,"collectNumber"));
        Topic hotTopic = new Topic();
        hotTopic.setStatus(1);//查出通过审核的状态为展示的专辑;
        ExampleMatcher exampleMatcher3 = ExampleMatcher.matching().withIgnorePaths("id","collectNumber","commentNumber");//long类型的需要忽略;
        Example<Topic> example3 = Example.of(hotTopic,exampleMatcher3);
        Page<Topic> hotTopicPage = topicService.findAllByExample(example3,pageable3);
        model.addAttribute("hotTopicPage",hotTopicPage);

        /**
         * 判断,根据前端如果传入分类则根据分类查询,否则查询所有;
         */
        if (null != request.getParameter("category")){

            long categoryId = Long.parseLong(request.getParameter("category"));
            /**
             * 将categoryId传回页面方便接下来的分页跳转;
             */
            model.addAttribute("categoryId",categoryId);

            /**
             * 通过用户点击的分类获取topic
             */
            Pageable pageable1 = new PageRequest(pn-1,10,new Sort(Sort.Direction.DESC,"creat_time"));
            Page<Topic> topicPage = topicService.findAllByCategoryIdAndStatus( categoryId,1, pageable1);//1 代表展示中的专辑;
            model.addAttribute("topicPage",topicPage);
        }else {
            /**
             * 获取所有的topic,用于在默认的没有选择分类的情况下;
             */
            Pageable pageable2 = new PageRequest(pn-1,10,  new Sort(Sort.Direction.DESC,"creatTime"));
            Topic allTopic = new Topic();
            allTopic.setStatus(1);//查出通过审核的状态为展示的专辑;
            ExampleMatcher exampleMatcher2 = ExampleMatcher.matching().withIgnorePaths("id","collectNumber","commentNumber");//long类型的需要忽略;
            Example<Topic> example2 = Example.of(allTopic,exampleMatcher2);
            Page<Topic> topicPage = topicService.findAllByExample(example2,pageable2);
            model.addAttribute("topicPage",topicPage);
        }

        return "portal/topic";
    }

    /**
     * 持久化用户投稿
     * @param request
     * @return
     */
    @RequestMapping(value = "/topic/contribute",method = RequestMethod.POST)
    @ResponseBody
    private Msg storyContribute(HttpServletRequest request){
        //从前端传来的请求中获取键为shopStr的值;
        String topicStr = RequestUtil.parserString(request, "topicStr");
        System.out.print("storyStr的值是:" + topicStr);
        ObjectMapper objectMapper = new ObjectMapper();
        Topic topic = null;
        try {
            //将字符串转成实体类
            topic = objectMapper.readValue(topicStr, Topic.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Msg.fail().setMsg("读取稿件信息失败!");
        }

        //从request中解析出上传的所有文件图片;
        Map<String, MultipartFile> fileMap = ((MultipartRequest) request).getFileMap();
        //单独拿出封面图片
        MultipartFile coverImg = fileMap.get("img");
        //再移除封面图片,剩下内容图片;//更新需求:将封面图一同打包进内容,页面上移除掉封面图展示;
        /*fileMap.remove("img");*/


        /**
         * 保存topic信息并获取返回的id值;
         */
        // TODO 这里先默认author id为1,后面改成从session中根据登录名获取user id;:
        User author = new User();
        author.setId(1);
        topic.setAuthor(author);
        topic.setCommentNumber(0);
        topic.setCollectNumber(0);
        topic.setCreatTime(new Date(System.currentTimeMillis()));
        String categoryId = request.getParameter("categoryId");//取出前端传入的categoryId,级联保存;
        Category category = new Category();
        category.setId(Long.parseLong(categoryId));
        topic.setCategory(category);
        Long topidId = 0L;
        try{
            topidId = topicService.saveAndFlush(topic);//先保存一次返回id用于文件夹隔离;
        }catch (Exception e){
            return Msg.fail().setMsg("投稿失败,保存稿件信息时出现异常");
        }
        if (null != topic && null != coverImg) {
            //设置中间文件夹,方便整理图片
            String centreAddr = "/topic/"+author.getId()+"/"+topidId+"/";
            //保存封面图片并返回地址;
            String imgAddr = ImgUtil.generateThumbnail(coverImg, centreAddr,1920, 1080);

            //用于接收遍历出的每一个图片文件;
            MultipartFile contentImg;
            //保存内容图片并返回地址,拼接成字符串方便存在数据库表中;;
            String contentImgsAddr = "";
            //新建一个String数组储存图片实际地址用于下面的zip打包传值;注意length一定要和fileMap实际要保存的图片数量保持一致,否则downloadZip类遍历时会抛空指针异常;
            String[] sourcePathArry = new String[fileMap.size()];
             for (int i = fileMap.size()-1 ; i>=0 ; i -- ){//这里注意有一张封面图的键是"img",不能用i取;倒序是为了保证封面图在第一;
                 if (fileMap.size()-1 == i){
                     contentImg = fileMap.get("img");
                 }else {
                     contentImg = fileMap.get(i+"");
                 }
                 String tempImgAddr = ImgUtil.generateThumbnail(contentImg, centreAddr, 1920, 1080)+",";
                    //新增zip压缩打包功能,所以将原本的字符直接 += 废弃掉;
                    sourcePathArry[i] = PathUtil.getImgBasePath() + tempImgAddr;
                    contentImgsAddr = contentImgsAddr+tempImgAddr;
            }
            //将最后一个","逗号去掉;
            contentImgsAddr = contentImgsAddr.substring(0, contentImgsAddr.length() - 1);
            /**
             * 开始储存topic实体的图片信息;
             */
            topic.setCoverImgAddr(imgAddr);
            topic.setContentImgsAddr(contentImgsAddr);



            /**
             * 新增zip打包功能,将用户上传的图片打包成zip,在页面提供zip下载地址;
             */
            try {// 将zip名字设置为"D:projectdev/images/upload/topic/{authouid}/{topicid}.zip"
                DownloadZip.downLoadZIP(PathUtil.getImgBasePath()+"/upload"+centreAddr+".zip",sourcePathArry);
                topic.setZipAddr("/upload"+centreAddr+".zip");
                topicService.saveAndFlush(topic);
            } catch (IOException e) {
                e.printStackTrace();
                return Msg.fail().setMsg("投稿失败,保存稿件信息时出现异常");
            }
            //返回注册店铺的最终结果;
            return Msg.success().setMsg("投稿成功,请等待审核");
        } else {
            return Msg.fail().setMsg("投稿失败,稿件信息读取不完整");
        }
    }

}
