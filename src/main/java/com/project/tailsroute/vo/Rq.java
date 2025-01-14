package com.project.tailsroute.vo;

import java.io.IOException;
import java.util.Map;

import com.project.tailsroute.service.MemberService;
import com.project.tailsroute.util.Ut;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class Rq {
    @Getter
    boolean isAjax;
    @Getter
    private boolean isLogined;
    @Getter
    private int loginedMemberId;
    @Getter
    private Member loginedMember;

    private HttpServletRequest req;
    private HttpServletResponse resp;

    private Map<String, String> paramMap;

    public Rq(HttpServletRequest req, HttpServletResponse resp, MemberService memberService) {
        this.req = req;
        this.resp = resp;

        paramMap = Ut.getParamMap(req);

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("loginedMemberId".equals(cookie.getName())) {
                    try {
                        loginedMemberId = Integer.parseInt(cookie.getValue());
                        isLogined = true;
                        loginedMember = memberService.getMemberById(loginedMemberId);
                    } catch (NumberFormatException e) {
                        loginedMemberId = 0;
                        isLogined = false;
                    }
                }
            }
        }

        this.req.setAttribute("rq", this);

        String requestUri = req.getRequestURI();

        boolean isAjax = requestUri.endsWith("Ajax");

        if (isAjax == false) {
            if (paramMap.containsKey("ajax") && paramMap.get("ajax").equals("Y")) {
                isAjax = true;
            } else if (paramMap.containsKey("isAjax") && paramMap.get("isAjax").equals("Y")) {
                isAjax = true;
            }
        }
        if (isAjax == false) {
            if (requestUri.contains("/get")) {
                isAjax = true;
            }
        }
        this.isAjax = isAjax;
    }

    public void printHistoryBack(String msg) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        println("<script>");
        if (!Ut.isEmpty(msg)) {
            println("alert('" + msg + "');");
        }
        println("history.back();");
        println("</script>");
    }

    private void println(String str) {
        print(str + "\n");
    }

    private void print(String str) {
        try {
            resp.getWriter().append(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        removeCookie("loginedMemberId");
    }

    public void removeCookie(String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

    public void login(Member member) {
        setCookie("loginedMemberId", String.valueOf(member.getId()));
    }

    public void setCookie(String cookieName, String value) {
        Cookie cookie = new Cookie(cookieName, value);
        cookie.setMaxAge(60 * 60 * 24);
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

    public void initBeforeActionInterceptor() {
        System.err.println("initBeforeActionInterceptor 실행");
    }

    public String historyBackOnView(String msg) {
        req.setAttribute("msg", msg);
        req.setAttribute("historyBack", true);
        return "usr/common/js";
    }

    public String getCurrentUri() {
        String currentUri = req.getRequestURI();
        String queryString = req.getQueryString();

        System.err.println(currentUri);
        System.err.println(queryString);

        if (currentUri != null && queryString != null) {
            currentUri += "?" + queryString;
        }

        System.out.println(currentUri);

        return currentUri;
    }

    public void printReplace(String resultCode, String msg, String replaceUri) {
        resp.setContentType("text/html; charset=UTF-8");
        print(Ut.jsReplace(resultCode, msg, replaceUri));
    }

    public String getEncodedCurrentUri() {
        return Ut.getEncodedCurrentUri(getCurrentUri());
    }

    public String getLoginUri() {
        return "../member/login?afterLoginUri=" + getAfterLoginUri();
    }

    public String getAfterLoginUri() {
        return getEncodedCurrentUri();
    }

    public String jsReplace(String msg, String uri) {
        return Ut.jsReplace(msg, uri);
    }

    public String getImgUri(int id) {
        return "/common/genFile/file/article/" + id + "/extra/Img/1";
    }

    public String getProfileFallbackImgUri() {
        return "https://via.placeholder.com/150/?text=*^_^*";
    }

    public String getProfileFallbackImgOnErrorHtml() {
        return "this.src = '" + getProfileFallbackImgUri() + "'";
    }

    public String getFindLoginIdUri() {
        return "../member/findLoginId?afterFindLoginIdUri=" + getAfterFindLoginIdUri();
    }

    private String getAfterFindLoginIdUri() {
        return getEncodedCurrentUri();
    }

    public String getFindLoginPwUri() {
        return "../member/findLoginPw?afterFindLoginPwUri=" + getAfterFindLoginPwUri();
    }

    private String getAfterFindLoginPwUri() {
        return getEncodedCurrentUri();
    }

    public boolean isAdmin() {
        if (isLogined == false) {
            return false;
        }

        return loginedMember.isAdmin();
    }
}