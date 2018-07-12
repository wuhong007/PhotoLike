TopTalk网站,照片 壁纸 绘画和涂鸦爱好者书写情感的地方.

使用SpringBoot搭建的项目,使用Mysql数据库,Spring Data JPA,Security,Redis,Thymeleaf等流行的框架和组件;

项目预览效果在根目录"项目页面效果预览"文件夹中;

最近喜欢在luoo网上听歌,感觉luoo网的前端页面设计得很简洁漂亮,板块全面而且功能不杂乱,虽然此网站是一个在线听歌的网站,但是板块却很适合用于论坛或博客等项目,于是我将页面全部另存为本地文件后准备改造成自己的论坛;

网站分为四个大板块:
     topic 专题壁纸分享板块(例如巴黎专辑,樱花专辑,建筑专辑) ; 
     essay 单张壁纸分享板块 (例如微软bing的每日一张壁纸推荐) , 
     story 有故事的系列壁纸分享板块(例如游记,电影截图影评) , 
     event 组织网友活动的板块(例如旅游,聚会,照相等)(计划改成推荐书刊杂志的板块);
     
目前用户可以在四个板块投稿,story板块和event板块的投稿引入了百度富文本编辑器,topic板块的投稿可以一次上传多张图片;

注意事项,项目中url地址中带/upload的是映射本地文件夹D:\projectdev\images\upload,所有图片会存在此文件夹内子文件夹中,可以先测试投稿页面再查看项目运行效果,否则部分页面板式会混乱或报错;

*********************** 开 发 记 录 ***************************

2018/7/12:

1:优化了一下页面的加载速度,废弃掉了百度和谷歌浏览量统计,以及一些无用的js代码和初始化操作;

2:新增了评论点赞功能,页面会加载评论后会根据用户是否赞过此评论来切换评论点赞按钮的状态,同时点赞计数也会根据用户的点赞动作而相应的增加或减少;

3:新增了Praise实体类以及数据库表praise,用于记录用户的点赞信息;Comment实体类新增praiseNumber属性用于记录被赞总数;

2018/7/11:

1:完成了topic essay 和story 三个板块收藏和取消收藏功能,同时各板块页面上的收藏按钮会根据用户是否收藏了此条目而显示"收藏"或"取消收藏"的对应样式;
    
2:修改了收藏夹页面的板式,使其图片宽和高的比例符合16:9,并自动缩放成固定的px大小;

            此次开发解决的问题:
                自定义删除sql语句时,需要注意注解一定要写全,三个注解都不能缺失,而查询语句只需要写@Query注解:
                @Modifying
                @Transactional
                @Query(nativeQuery = true, value = "delete from user_favorite where  user_id= :userId and story_id=:storyId"
                    )
                void deleteFavoriteStory(@Param("userId")Long userId,@Param("storyId")Long storyId );

2018/7/10:

1:修改了专辑图片的投稿逻辑:将封面图片与内容图片投稿时一同打包,并按专辑id做为文件夹名进行隔离;在展示专辑图片滚动播放时,将专辑图作为第一张展示,同时设置投稿功能需要security权限验证;

2:移除了百度浏览数据流量统计的js代码;

2018/7/09:

1:完成了父评论下的子评论的分页功能;实现方式是利用js控制加载的数量,默认只加载10个,当用户点击"更多子评论"按钮时,会再加载后面的10个,此时父评论下就累积了20个子评论,并判断是否还有剩余评论,有则继续显示此按钮,没有则改变此按钮样式;
  当子评论加载超过10个时,显示"折叠子评论"按钮,点击后则刷新掉超出10个的部分,变成初始10个的数量,并移除掉此按钮,同时判断是否要显示"更多子评论"按钮;
  
2:修改了专辑页面的板式,移除了封面图片的展示,将其添加进专辑内容展示区域并且包含进下载的zip压缩包中;

2018/7/08:

1:完成了专辑评论和故事评论 以及父评论下面的子评论功能,需要以后完善子评论的翻页功能(由于落网上子评论数量较少没有达到翻页的限制,所以没有展示子评论翻页的样式作为参考,前端页面样式设计会比较麻烦);

2:修改了项目session的有效期为6个小时,防止当用户投稿时编辑时间过长导致发布失败;

2018/7/07:

1:完成了单独修改用户头像,单独修改用户签名,以及修改用户其它信息的功能,暂时不提同修改密码的功能,因为考虑到以后还需要密码加密储存,所以以后再完成修改密码的功能;

2:删掉了较为累赘的Comment.js文件,重写评论功能的js代码;

2018/7/06:

1:完成了修改用户信息的页面,鼠标移动到用户头像上时会弹出"修改头像"的提示按钮,点击后弹出模态框,在模态框里可以上传图片,可以预览和进行裁剪;

2:后端新增接收base64字符串转换成multipartFile文件的功能,原理是新增自定义类继承MultipartFile类,重点提取请求头信息和base64字符串转成byte数组,这两个之外的属性(例如文件名和原始文件名等)采用随机数生成;

2018/7/05:

1:整合了security,新增角色实体类Role,角色与用户关联的实体类RoleRelateUser,数据库新增role表和role_user表;
同时提交了测试用sql数据库数据文件;

2:完成了用户登录和用户注册功能;当用户登录时security会从数据库中找到用户账号对应的角色信息判断是否具有对应网页的权限;

3:移除了弹出式登录框和注册框,改为登录页面和注册页面,这是为了更好的配合security权限组件;

2018/7/04:

1:修改了一下用户收藏页面和用户投稿查看页面;

2018/7/03:

1:完成了活动投稿功能;

2:完善了活动城市分类筛选功能,完善了活动详情页及列表页页面显示效果;

2018/7/02:

1:新增在页面导航条上点击登录链接时弹出登录框,点击关闭则关闭登录框;

2:新增注册框,登录和注册功能暂时放到后面与security一起完成;

2018/7/01:

1:新增了文件上传时自动打包成zip文件的功能,同时设置所有上传图片以1080p分辨率保存;专辑页面新增下载zip文件的功能,点击后可以下载专辑页面所有图片的zip压缩包;随笔页面可以点击图片查看原图,故事页面可以右击图片保存原图;

2:完善了各板块的页面显示效果;将上次强制缩放为1.42比例的图片更改为1080P的16:9的图片,这样用户就可以下载高清的图片作为壁纸,同时也可以上传自己的壁纸,板块开发需求从侧重文字抒情改为分享精美壁纸;

2018/6/30:

1:完成了随笔投稿功能,用户可以投稿随笔,撰写随笔标题,上传随笔封面图片,撰写随笔正文,目前会强制缩放用户上传的图片,将其缩放成长宽比为1.42的矩形,在网上找了一下页面裁剪图片的插件,发现都是js记录并发送裁剪的数据和原图,还需要后台去处理裁剪数据,由于时间不够充裕,所以留着以后完善此功能;

2:完成了故事和专辑的投稿功能;

3:新增了专辑页面的轮播图功能;

4:废弃掉了专辑与分类的多对多的关系,修改为专辑与分类的多对一关系,因此修改了相关实体类与数据库表结构;

2018/6/29:

1:发现Umeditor 存在段落样式无效的问题,已经换成了完整版的Ueditor,测试段落样式可以有效使用;

2:完成了Ueditory富文本编辑器的图片上传保存和回显;

2018/6/28:

1:完成了专辑投稿页面的前端;使用js完成了封面单图选择和回显,以及多图片上传的选择和回显,

2:完成了故事投稿页面的百度富文本编辑器Umeditor 的前端页面整合;

2018/6/27:

1:完成了topic和story板块的评论加载和子评论加载;父评论翻页以完成,子评论还没有完成翻页功能;

2:修复了当查询的记录为0时topic和story总页面尾页参数为0的错误;

2018/6/26:

1:在开发查询用户信息时发现jpa的example查询会查出不区分大小写的记录;
        
        //创建的example,withMatcher()已经设置了属性account区分大小写,然后又将整个example的withIgnoreCase()忽略大小写设置成了false,
            ExampleMatcher exampleMatcher = ExampleMatcher.matching().withMatcher("account",ExampleMatcher.GenericPropertyMatchers.caseSensitive()).withIgnorePaths("id").withIgnoreCase(false);
        //然而还是查询多条不区分大小写的记录,原因在于mysql数据库查询时不区分大小写;
            报错信息:query did not return a unique result: 3; nested exception is javax.persistence.NonUniqueResultException: query did not return a unique result: 3
        //解决方法,以下方法任选一个即可:
        //1:在查询语句中加上binary;  
            select * from user where account = binary 'testuser'
        //2:在mysql设置文件里开启区别大小写;可以修改my.ini或者my.cnf 
                [mysqld] 
                lower_case_table_names=1 
                （0：区分；1：不区分）
        //3:修改表中的字段使其区别大小写;
            alter table user change account account varchar(225) binary;
  
2:完成了浏览用户发布的专辑,随笔,故事三个页面,并完善了一下用户收藏页面的逻辑,当查询不到记录时显示提示;     

2018/6/25:

1:完成了用户信息里的随笔收藏,专辑收藏,故事收藏这三个板块的浏览页面;

2018/6/24:

1:完成了各个板块除评论功能外基本的页面渲染及链接的跳转;

2018/6/23:

1:完成了通过分类id查询topic并且返回page<Topic>的功能,解决的问题:开发时遇到自定义查询能完成查询但是却不能分页,总是查询出所有的记录,在网上搜索了很多遇到相同问题的记录,后来有网友解释早先版本是因为bug问题不能按照官方文档上的说明来实现分页,解决方案是在语句后加order by ?#{#pageable}来实现分页,但是从spring 2.0.4版本之后修复了这个bug,分页排序可以直接传pageable,加了这段语句反而不能分页,
        
        spring2.0.4之前需要在value语句后加ORDER BY ?#{#pageable}并且在接口方法里传pageable来实现分页和排序 ;
        spring2.0.4之后直接传pageable即可,再加ORDER BY ?#{#pageable}反而不能分页排序;       
         @Query(nativeQuery = true, value = "select * from topic  where id in (select topic_id from category_topic where category_id= :categoryId)",//ORDER BY ?#{#pageable}
                    countQuery = "select count(*) from topic  where id in (select topic_id from category_topic where category_id= :categoryId)")
            Page<Topic> findByCategoryId(@Param("categoryId")Long categoryId,Pageable pageable);

2018/6/22:

1:经过跟网友的讨论,重新整理了实体类,废弃了多对多的级联映射,以提高性能及数据操作的灵活性;

2:在数据库中重新注入了测试数据;

3:实现了首页及专辑页的不带条件的查询的页面显示,接下来需要实现点击分类后进行分页查询并显示;

2018/6/21 : 

1:根据前端页面及功能设计了一些实体类,通过hibernate注解配置好了实体类及实体类之间的级联关系,生成了数据库表;

2:增删改查已经通过测试,目前发现的问题:在Java9中已经移除了部分jarEE包导致hibernate会报错java.lang.ClassNotFoundException: javax.xml.bind.JAXBException
       
                
                <!--下面四个jar包是由于java9移除了这些jarEE包导致hibernate运行时抛出java.lang.ClassNotFoundException: javax.xml.bind.JAXBException-->
                <dependency>
                    <groupId>javax.xml.bind</groupId>
                    <artifactId>jaxb-api</artifactId>
                    <version>2.3.0</version>
                </dependency>
                <dependency>
                    <groupId>com.sun.xml.bind</groupId>
                    <artifactId>jaxb-impl</artifactId>
                    <version>2.3.0</version>
                </dependency>
                <dependency>
                    <groupId>com.sun.xml.bind</groupId>
                    <artifactId>jaxb-core</artifactId>
                    <version>2.3.0</version>
                </dependency>
                <dependency>
                    <groupId>javax.activation</groupId>
                    <artifactId>activation</artifactId>
                    <version>1.2.0</version>
                </dependency>
另外在执行批量save时,注意saveAndFlush会返回id导致批量持久化会报错说重复的id字段;批量save时注意要在for循环里new对象才能保证id为空,否则只会不停的update数据导致只保存了一条记录;

2018/6/20 : 创建项目,导入前端页面;
