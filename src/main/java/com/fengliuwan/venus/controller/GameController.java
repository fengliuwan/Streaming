package com.fengliuwan.venus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fengliuwan.venus.service.GameService;
import com.fengliuwan.venus.service.TwitchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//mark class's role as a web component,
//so the spring mvc will register the methods which annotated the @RequestMapping.
@Controller
public class GameController {

    private GameService gameService;

    @Autowired
    public GameController (GameService gameService) {
        this.gameService = gameService;
    }


    //method name does not have to be doGet as in servlet
    //game?game_name=xxx @RequestParam get xxx from http request
    //Use the @RequestMapping annotation to define REST API, such as HTTP URL, method, etc.
    @RequestMapping(value = "/game", method = RequestMethod.GET)
    public void getGame(@RequestParam(value = "game_name", required = false) String gameName,
                        HttpServletResponse response) throws IOException , ServletException {

        response.setContentType("application/json; charset=UTF-8");
        try {
            // Return the dedicated game information if gameName is provided in the request URL, otherwise return the top x games.
            if (gameName != null) {
                response.getWriter().print(new ObjectMapper().writeValueAsString(gameService.searchGame(gameName)));
            } else {
                response.getWriter().print(new ObjectMapper().writeValueAsString(gameService.topGames(0)));
            }
        } catch (TwitchException e) {
            throw new ServletException(e);
        }
    }
}
