package com.project.tailsroute.controller;

import com.project.tailsroute.service.*;
import com.project.tailsroute.util.Ut;
import com.project.tailsroute.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UsrArticleController {

    private final Rq rq;

    public UsrArticleController(Rq rq) {
        this.rq = rq;
    }

    @Autowired
    private ArticleService articleService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private ReactionPointService reactionPointService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private WalkService walkService;

    @PostMapping("/usr/article/uploadImage")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        String UPLOAD_DIR = "uploads/photo/";
        int id = articleService.getCurrentArticleId();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
        }

        try {
            // 저장 경로 확인 및 폴더 생성
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 파일 저장
            String fileName = System.currentTimeMillis() + "_" + "article" + id + ".png";
            String filePath = UPLOAD_DIR + fileName;
            Files.write(Paths.get(filePath), file.getBytes());

            // URL 반환
            String fileUrl = "/" + UPLOAD_DIR + fileName;
            return ResponseEntity.ok().body(Map.of("url", fileUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("파일 업로드 중 오류 발생");
        }
    }

    @GetMapping("/usr/article/detail")
    public String showDetail(Model model, int id) {

        boolean isLogined = rq.isLogined();
        if (isLogined) {
            Member member = rq.getLoginedMember();
            model.addAttribute("member", member);
        }

        model.addAttribute("isLogined", isLogined);

        Article article = articleService.getForPrintArticle(rq.getLoginedMemberId(), id);

        if (article == null) {
            return "redirect:/usr/article/main";
        }

        // System.err.println(id + "번 글");
        // System.err.println(rq.getLoginedMemberId() + "번 회원 접속중");
        // System.err.println("내용" + article);

        model.addAttribute("article", article);

        List<Reply> replies = replyService.getForPrintReplies(rq.getLoginedMemberId(), "article", id);

        int repliesCount = replies.size();
        model.addAttribute("replies", replies);
        model.addAttribute("repliesCount", repliesCount);

        model.addAttribute("isAlreadyAddGoodRp",
                reactionPointService.isAlreadyAddGoodRp(rq.getLoginedMemberId(), id, "article"));
        model.addAttribute("isAlreadyAddBadRp",
                reactionPointService.isAlreadyAddBadRp(rq.getLoginedMemberId(), id, "article"));

        return "usr/article/detail";
    }

    @PostMapping("/usr/article/doIncreaseHitCountRd")
    @ResponseBody
    public ResultData doIncreaseHitCount(int id) {

        ResultData increaseHitCountRd = articleService.increaseHitCount(id);

        if (increaseHitCountRd.isFail()) {
            return increaseHitCountRd;
        }

        ResultData rd = ResultData.newData(increaseHitCountRd, "hitCount", articleService.getArticleHitCount(id));

        rd.setData2("조회수가 증가된 게시글의 id", id);

        return rd;
    }

    @GetMapping("/usr/article/modify")
    public String showModify(Model model, int id) {

        boolean isLogined = rq.isLogined();
        if (isLogined) {
            Member member = rq.getLoginedMember();
            model.addAttribute("member", member);
        } else {
            return "redirect:/usr/member/login";
        }
        model.addAttribute("isLogined", isLogined);

        Article article = articleService.getForPrintArticle(rq.getLoginedMemberId(), id);


        ResultData userCanModifyRd = articleService.userCanModify(rq.getLoginedMemberId(), article);


        if (article == null || userCanModifyRd.isFail()) {
            return "redirect:/usr/article/main";
        }

        model.addAttribute("article", article);

        return "/usr/article/modify";
    }

    // 로그인 체크 -> 유무 체크 -> 권한 체크 -> 수정
    @PostMapping("/usr/article/doModify")
    @ResponseBody
    public String doModify(int boardId, int id, String title, String body) {

        Article article = articleService.getArticleById(id);

        if (article == null) {
            return Ut.jsHistoryBack("F-1", Ut.f("%d번 게시글은 없습니다", id));
        }

        ResultData userCanModifyRd = articleService.userCanModify(rq.getLoginedMemberId(), article);

        if (userCanModifyRd.isFail()) {
            return Ut.jsHistoryBack(userCanModifyRd.getResultCode(), userCanModifyRd.getMsg());
        }

        if (userCanModifyRd.isSuccess()) {
            articleService.modifyArticle(boardId, id, title, body);
        }

        return Ut.jsReplace(userCanModifyRd.getResultCode(), userCanModifyRd.getMsg(), "../article/detail?id=" + id);
    }

    @GetMapping("/usr/article/doDelete")
    @ResponseBody
    public String doDelete(int id) {

        Article article = articleService.getArticleById(id);

        if (article == null) {
            return Ut.jsHistoryBack("F-1", Ut.f("%d번 게시글은 없습니다", id));
        }

        ResultData userCanDeleteRd = articleService.userCanDelete(rq.getLoginedMemberId(), article);

        if (userCanDeleteRd.isFail()) {
            return Ut.jsHistoryBack(userCanDeleteRd.getResultCode(), userCanDeleteRd.getMsg());
        }

        if (userCanDeleteRd.isSuccess()) {
            articleService.deleteArticle(id);
        }

        return Ut.jsReplace(userCanDeleteRd.getResultCode(), userCanDeleteRd.getMsg(), "../article/main");
    }

    @GetMapping("/usr/article/write")
    public String showWrite(Model model) {

        boolean isLogined = rq.isLogined();
        if (isLogined) {
            Member member = rq.getLoginedMember();
            model.addAttribute("member", member);
        } else {
            return "redirect:/usr/member/login";
        }
        model.addAttribute("isLogined", isLogined);

        Integer currentId = articleService.getCurrentArticleId();
        if (currentId == null) {
            currentId = 1;
        }

        model.addAttribute("currentId", currentId);

        return "usr/article/write";
    }

    @PostMapping("/usr/article/doWrite")
    @ResponseBody
    public String doWrite(String boardId, String title, String body) {

        if (Ut.isEmptyOrNull(title)) {
            return Ut.jsHistoryBack("F-1", "제목을 입력해주세요");
        }
        if (Ut.isEmptyOrNull(body)) {
            return Ut.jsHistoryBack("F-2", "내용을 입력해주세요");
        }
        if (Ut.isEmptyOrNull(boardId)) {
            return Ut.jsHistoryBack("F-3", "게시판을 선택해주세요");
        }

        // System.err.println(boardId);

        ResultData writeArticleRd = articleService.writeArticle(rq.getLoginedMemberId(), title, body, boardId);

        int id = (int) writeArticleRd.getData1();

        return Ut.jsReplace(writeArticleRd.getResultCode(), writeArticleRd.getMsg(), "../article/detail?id=" + id);

    }

    @GetMapping("/usr/article/main")
    public String showMain(Model model, @RequestParam(defaultValue = "0") int boardId) {

        boolean isLogined = rq.isLogined();
        if (isLogined) {
            Member member = rq.getLoginedMember();
            model.addAttribute("member", member);
        }
        model.addAttribute("isLogined", isLogined);

        List<Article> articles = articleService.getMainArticles(boardId);

        for (Article article : articles) {
            article.setPoto(articleService.extractFirstImageSrc(article.getBody()));
            article.setBody(articleService.removeHtmlTags(article.getBody()));
            article.setArticleCanNew(articleService.isNew(article.getRegDate()));
        }

        model.addAttribute("articles", articles);
        model.addAttribute("boardId", boardId);

        List<Walk> walks = walkService.getWalksRanking();

        Map<Integer, String> monthTitles = new HashMap<Integer, String>();
        monthTitles.put(1, "새해를 여는 산책왕");
        monthTitles.put(2, "설렘 가득한 산책왕");
        monthTitles.put(3, "환히 빛나는 산책왕");
        monthTitles.put(4, "꽃길 걷는 산책왕");
        monthTitles.put(5, "뭘 좀 아는 산책왕");
        monthTitles.put(6, "초여름의 산책왕");
        monthTitles.put(7, "여름 햇살 속 산책왕");
        monthTitles.put(8, "자유로운 산책왕");
        monthTitles.put(9, "산책을 즐기는 왕");
        monthTitles.put(10, "단풍길 위 산책왕");
        monthTitles.put(11, "늦가을 감성 산책왕");
        monthTitles.put(12, "눈 내리는 산책왕");

        int currentMonth = LocalDate.now().getMonthValue();
        String monthlyTitle = monthTitles.getOrDefault(13, "이달의 산책왕");

        model.addAttribute("monthlyTitle", monthlyTitle); // 현재 월의 제목 전달
        model.addAttribute("walks", walks);
        System.err.println("walks : " + walks);

        String randomMotivation = articleService.getRandomMotivation();
        model.addAttribute("randomMotivation", randomMotivation);

        return "usr/article/main";
    }

    @GetMapping("/usr/article/list")
    public String showList(Model model, @RequestParam(defaultValue = "0") int boardId,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "전체") String searchKeywordTypeCode,
                           @RequestParam(defaultValue = "") String searchKeyword,
                           @RequestParam(defaultValue = "0") int memberId,
                           @RequestParam(defaultValue = "regDate") String sortOrder) {

        boolean isLogined = rq.isLogined();
        if (isLogined) {
            Member member = rq.getLoginedMember();
            model.addAttribute("member", member);
        }
        model.addAttribute("isLogined", isLogined);

        Board board = boardService.getBoardById(boardId);

        int articlesCount = articleService.getArticlesCount(boardId, searchKeywordTypeCode, searchKeyword, memberId);

        // 한페이지에 글 10개
        // 글 20개 -> 2page
        // 글 25개 -> 3page
        int itemsInAPage = 10;

        int pagesCount = (int) Math.ceil(articlesCount / (double) itemsInAPage);

        List<Article> articles = articleService.getForPrintArticles(boardId, itemsInAPage, page, searchKeywordTypeCode,
                searchKeyword, memberId, sortOrder);

        for (Article article : articles) {
            article.setPoto(articleService.extractFirstImageSrc(article.getBody()));
            article.setBody(articleService.removeHtmlTags(article.getBody()));
            article.setArticleCanNew(articleService.isNew(article.getRegDate()));
        }

        model.addAttribute("articles", articles);
        model.addAttribute("articlesCount", articlesCount);
        model.addAttribute("pagesCount", pagesCount);
        model.addAttribute("board", board);
        model.addAttribute("page", page);
        model.addAttribute("searchKeywordTypeCode", searchKeywordTypeCode);
        model.addAttribute("searchKeyword", searchKeyword);
        model.addAttribute("boardId", boardId);
        model.addAttribute("memberId", memberId);
        model.addAttribute("sortOrder", sortOrder);

        return "usr/article/list";
    }
}