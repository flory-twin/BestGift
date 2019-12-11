package co.grandcircus.bestgift.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import co.grandcircus.bestgift.GiftService;
import co.grandcircus.bestgift.jparepos.GiftListRepository;
import co.grandcircus.bestgift.jparepos.KeywordRepository;
import co.grandcircus.bestgift.jparepos.SearchExpressionRepository;
import co.grandcircus.bestgift.models.GiftResult;
import co.grandcircus.bestgift.models.Image;
import co.grandcircus.bestgift.search.Keyword;
import co.grandcircus.bestgift.search.Operator;
import co.grandcircus.bestgift.search.SearchExpression;

@Controller
public class GiftController {
	@Value("${etsy.key}")
	private String etsyKey;

	@Autowired
	GiftService gs;
	@Autowired 
	GiftListRepository gl;
	@Autowired
	SearchExpressionRepository ser;
	@Autowired
	KeywordRepository kr;

	@RequestMapping("/")
	public ModelAndView routeFromIndex(HttpSession session) {
		recacheRepositories(session);
		return viewGifts(session);
	}

	/**
	 * Routes traffic to the entry page, giftresults.jsp.
	 * 
	 * 
	 * @param session
	 * @return
	 */
	@RequestMapping("/gift-results")
	public ModelAndView viewGifts(HttpSession session) {
		Image imgResult;
		int listId;
		String imageUrl;
		ModelAndView mv = new ModelAndView("giftresults");

		String url = "https://openapi.etsy.com/v2/listings/active?api_key=" + etsyKey;
		
		recacheRepositories(session);
		
		GiftResult result = gs.getListOfGifts();
		recacheResult(result, session);

		return mv;

	}

	@RequestMapping("/etsy-results")
	public ModelAndView SearchGifts(
			HttpSession session, 
			@RequestParam String keywords, 
			@RequestParam(required = false) String keywords2,
			@RequestParam(required = false) String keywords3,
			@RequestParam(required = false) String keywords4,
			@RequestParam Double max_price) {
		// Just in case user navigated straight to this page...
		recacheRepositories(session);
		
		//request.getParameter("product"+i+"SkusCnt"))
		
		ModelAndView mv = new ModelAndView("giftresults");
		// Put search operators into repo
		Keyword k = addKeyword(keywords);
		// TODO for later: move all DB stuff into Service, or move it here, but not half and half
		SearchExpression searchExp = new SearchExpression(k);
		
		if (keywords2 != null && keywords2 != "") {
			SearchExpression inner2 = new SearchExpression(addKeyword(keywords2));
			if (keywords3 != null && keywords3 != "") {
				SearchExpression inner3 = new SearchExpression(addKeyword(keywords3));
				if (keywords4 != null && keywords4 != "") {
					inner3.setO(Operator.AND);
					inner3.setK2(addKeyword(keywords4));
				}
				inner2.setO(Operator.AND);
				inner2.setBaseSE(inner3);
			}
			searchExp.setO(Operator.AND);
			searchExp.setBaseSE(inner2);
		}
		
		
		// Perform actual search 
		// TODO: Refactor to take SearchExp
		
		GiftResult result = gs.getListOfSearchedGifts(searchExp);
		
		// Cache new results.
		this.recacheResult(result, session);

		//mv.addObject("giftresult", result.getResults());

		return mv;

	}
	
	@RequestMapping("/etsy-results2")
	public ModelAndView SearchGifts(HttpSession session, @RequestParam String keywords, @RequestParam String keywords2) {
		// Just in case user navigated straight to this page...
		recacheRepositories(session);
		
		ModelAndView mv = new ModelAndView("TestOutPut");
		// Put search operators into repo
		Keyword k = addKeyword(keywords);
		Keyword k2 = addKeyword(keywords2);
		// TODO for later: move all DB stuff into Service, or move it here, but not half and half
		SearchExpression searchExp = addSearchExpression(k, k2);
		
		
		// Perform actual search 
		// TODO: Refactor to take SearchExp
		GiftResult result = gs.getListOfSearchedGifts(searchExp);
		
		// Cache new results.
		this.recacheResult(result, session);

		//mv.addObject("giftresult", result.getResults());

		return mv;

	}

	@RequestMapping("/image")
	public ModelAndView giftImages(String listing_id) {
		ModelAndView mv = new ModelAndView("TestOutPut");

		Image result = gs.getGiftImage(listing_id);

		mv.addObject("i", result);
		return mv;

	}

	@RequestMapping("/image/newSearch")
	public ModelAndView giftImagesNoUrl() {
		return new ModelAndView("TestOutPut");
	}

	@RequestMapping("/search")
	public ModelAndView searchSingleKeyword(String kw1, HttpSession session) {
		recacheRepositories(session);
		
//		List<Gift> lastRoundOfGifts = ((List<Gift>) session.getAttribute("currentGiftList"));
//		Searcher seekAmongGifts = new Searcher(lastRoundOfGifts);
//		session.setAttribute("currentGiftList", seekAmongGifts.findMatchingGifts(new Keyword(kw1)));
//		
//		List<Gift> lastRoundOfGifts = ((List<Gift>) session.getAttribute("currentGiftList"));
//		Keyword k = addKeyword(kw1);
//		Searcher seekAmongGifts = new KeywordSearcher(lastRoundOfGifts, k);
//		session.setAttribute("currentGiftList", seekAmongGifts.findMatchingGifts());

		return new ModelAndView("giftresults");
	}
	
	@RequestMapping("/search-history")
	public ModelAndView showHistoryPage(HttpSession session, @RequestParam(required = false) Integer listId) {
		if (listId == null) {
			return new ModelAndView("searchhistory");
		} else {
			return new ModelAndView("searchhistory", "listId", listId);
		}
	}
	
	private void recacheResult(GiftResult toCache, HttpSession session) {
		session.setAttribute("result", toCache);
		session.setAttribute("currentGiftList", toCache.getResults());
	}
	private void recacheRepositories(HttpSession session) {
		session.setAttribute("gs", gs);
		session.setAttribute("gl", gl);
	}

	private Keyword addKeyword(String value) {
		Keyword k = new Keyword(value);
		kr.save(k);
		return k;
	}
	
	private SearchExpression addSearchExpression(Keyword k) {
		SearchExpression searchExp = new SearchExpression(k);
		ser.save(searchExp);
		return searchExp;
	}
	
	private SearchExpression addSearchExpression(Keyword k1, Keyword k2) {
		SearchExpression searchExp = new SearchExpression(k1, Operator.AND, k2);
		ser.save(searchExp);
		return searchExp;
	}
	
	private SearchExpression addSearchExpression(SearchExpression se) {
		ser.save(se);
		return se;
	}
}
