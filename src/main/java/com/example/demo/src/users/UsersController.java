package com.example.demo.src.users;

import com.example.demo.config.BaseException;
import com.example.demo.config.BaseResponse;
import com.example.demo.config.BaseResponseStatus;
import com.example.demo.src.user.model.KakaoProfile;
import com.example.demo.src.users.model.*;
import com.example.demo.utils.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static com.example.demo.config.BaseResponseStatus.*;
import static com.example.demo.utils.ValidationRegex.isRegexEmail;

@RestController
@RequestMapping("/users")
public class UsersController {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private final UsersProvider usersProvider;
    @Autowired
    private final UsersService usersService;

    @Autowired
    private final JwtService jwtService;

    public UsersController(UsersProvider usersProvider,UsersService usersService,JwtService jwtService){
        this.usersProvider=usersProvider;
        this.usersService=usersService;
        this.jwtService=jwtService;

    }

    //ํ์ ์กฐํ
    @ResponseBody
    @GetMapping("/{userId}")
    public BaseResponse<GetUsersRes> getUser(@PathVariable("userId") int userId) throws BaseException {
//        try{
//            GetUsersRes getUsersRes = usersProvider.getUser(userId);
////            return new BaseResponse<>(getUsersRes);
//            return new BaseResponse<>(getUsersRes);
//        }catch (SQLException e){
//            return new BaseResponse<>(BaseResponseStatus.DATABASE_ERROR);
//        }
            return new BaseResponse<>(usersProvider.getUser(userId));

//        GetUsersRes getUsersRes = usersProvider.getUser(userId);


    }

    //ํ์๊ฐ์
    @ResponseBody
    @PostMapping("")
    public BaseResponse createUser(@RequestBody PostUsersReq postUsersReq) throws BaseException {
        if(postUsersReq.getEmail() == null){
            return new BaseResponse<>(POST_USERS_EMPTY_EMAIL);
        }
        //์ด๋ฉ์ผ ์?๊ทํํ
        if(!isRegexEmail(postUsersReq.getEmail())){
            return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
        }
            PostUsersRes postUsersRes = usersService.createUser(postUsersReq);
            return new BaseResponse<>(postUsersRes);
    }

    List<String> patchList = Arrays.asList("email","pwd","phoneNum");
    //ํ์ ์?๋ณด ์์?
    @ResponseBody
    @PatchMapping("")
    public BaseResponse<String> modifyUser(@RequestBody PatchUsersReq patchUsersReq) throws BaseException {
            if (!(patchList.contains(patchUsersReq.getModItem()))) {
                return new BaseResponse<>(REQUEST_ERROR);
            }
            usersService.modifyUser(patchUsersReq);
            String result="";
            return new BaseResponse<>(result);

    }

    @ResponseBody
    @DeleteMapping("/{userId}")
    public BaseResponse<String> DeleteUser(@PathVariable("userId") int userId) throws BaseException {
            usersService.DeleteUser(userId);
            String result="";
            return new BaseResponse<>(result);
    }

    @ResponseBody
    @PostMapping("/login")
    public BaseResponse<PostLoginRes> logIn(@RequestBody PostLoginReq postLoginReq) throws BaseException {
            PostLoginRes postLoginRes = usersProvider.logIn(postLoginReq);
            return new BaseResponse<>(postLoginRes);
    }

    @ResponseBody    //๋ฐ์ดํฐ๋ฅผ ๋ฆฌํดํด์ฃผ๋ ์ปจํธ๋กค๋ฌ ํจ์
    @GetMapping("/auth/kakao/callback")
    public BaseResponse<String> kakaoCallback(String code) throws JsonProcessingException {

        //HttpHeader ์ค๋ธ์?ํธ ์์ฑ
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type","application/x-www-form-urlencoded;charset=utf-8");


        //HttpBody ์ค๋ธ์?ํธ ์์ฑ
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("grant_type","authorization_code");
        params.add("client_id","2800be50153ad37ba3105623ca5d5dfc");
        params.add("redirect_uri","http://localhost:9000/users/auth/kakao/callback");
        params.add("code",code);

        //HttpHeader์ HttpBody๋ฅผ ํ๋์ ์ค๋ธ์?ํธ์ ๋ด๊ธฐ
        //exchange ํจ์๊ฐ HttpEntity ํ์์ ๋ฃ๊ฒ ๋์ด์์ด์
        HttpEntity<MultiValueMap<String,String>> kakaoTokenRequest =
            new HttpEntity<>(params, headers);

        //Http ์์ฒญํ๊ธฐ - Post ๋ฐฉ์์ผ๋ก - ๊ทธ๋ฆฌ๊ณ? response ๋ณ์์ ์๋ต ๋ฐ์
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        OAuthToken oAuthToken = objectMapper.readValue(response.getBody(), OAuthToken.class);
        System.out.println("์นด์นด์ค ์์ธ์ค ํ?ํฐ: "+oAuthToken.getAccess_token());

        //HttpHeader ์ค๋ธ์?ํธ ์์ฑ
        RestTemplate rt2 = new RestTemplate();
        HttpHeaders headers2 = new HttpHeaders();
        headers2.add("Authorization"," Bearer "+ oAuthToken.getAccess_token());
        headers2.add("Content-type","application/x-www-form-urlencoded;charset=utf-8");

        //HttpHeader์ HttpBody๋ฅผ ํ๋์ ์ค๋ธ์?ํธ์ ๋ด๊ธฐ
        HttpEntity<MultiValueMap<String,String>> kakaoProfileRequest2 =
                new HttpEntity<>(headers2);

        //Http ์์ฒญํ๊ธฐ - Post ๋ฐฉ์์ผ๋ก - ๊ทธ๋ฆฌ๊ณ? response ๋ณ์์ ์๋ต ๋ฐ์
        ResponseEntity<String> response2 = rt2.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoProfileRequest2,
                String.class
        );

        ObjectMapper objectMapper2 = new ObjectMapper();
        KakaoProfile kakaoProfile = objectMapper2.readValue(response2.getBody(), KakaoProfile.class);
        System.out.println("์นด์นด์ค ์์ด๋(๋ฒํธ): "+kakaoProfile.getId());
        System.out.println("์นด์นด์ค ์ด๋ฉ์ผ: "+ kakaoProfile.getKakao_account().getEmail());
        return new BaseResponse<>(response2.getBody());
    }




}
