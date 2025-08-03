package com.hust.baseweb.rest.user;

import com.hust.baseweb.applications.programmingcontest.model.ModelSearchUserResult;
import com.hust.baseweb.service.UserService;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @Secured("ROLE_TEACHER")
    @GetMapping("/users")
    public ResponseEntity<?> search(
        Pageable pageable,
        @Param("keyword") String keyword,
        @RequestParam(required = false, name = "exclude") List<String> excludeIds
    ) {
        if (keyword == null) {
            keyword = "";
        }

        if (excludeIds == null) {
            excludeIds = Collections.emptyList();
        }

        Page<ModelSearchUserResult> resp = userService.search(keyword, excludeIds, pageable);
        return ResponseEntity.status(200).body(resp);
    }

    /**
     * It returns a list of users, with pagination, and the ability to search by name
     *
     * @param page         This is a Pageable object that is used to paginate the results.
     * @param searchString The search string that the user entered in the search box.
     * @return A list of users
     */
    @Secured("ROLE_TEACHER")
    @GetMapping(path = "/statistics/users")
    public ResponseEntity<?> getUsers(
        Pageable page,
        @RequestParam(name = "search", required = false) String searchString
    ) {
        Page<ModelSearchUserResult> res = userService.search(searchString, Collections.emptyList(), page);
        return ResponseEntity.ok().body(res);
    }

    /**
     * > This function returns a list of all enabled user login ids that contain the given string
     *
     * @param partOfLoginId The part of the login id that you want to search for.
     * @param limit         The maximum number of results to return.
     * @return A list of all enabled user login ids that contain the search string.
     */
    @Secured("ROLE_TEACHER")
    @GetMapping("/user-login-ids")
    public ResponseEntity<?> getEnabledUserLoginIds(
        @RequestParam(name = "search", required = false, defaultValue = "") String partOfLoginId,
        @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit
    ) {
        return ResponseEntity.ok(userService.getAllEnabledLoginIdsContains(partOfLoginId, limit));
    }
}
