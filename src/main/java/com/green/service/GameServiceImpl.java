package com.green.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.domain.CompanyVO;
import com.green.domain.GamePictureVO;
import com.green.domain.GameResourceVO;
import com.green.domain.GameVO;
import com.green.domain.RatingVO;
import com.green.domain.SpecVO;
import com.green.domain.TagVO;
import com.green.mapper.CompanyMapper;
import com.green.mapper.GameMapper;
import com.green.mapper.GamePictureMapper;
import com.green.mapper.GameResourceMapper;
import com.green.mapper.RatingMapper;
import com.green.mapper.SpecMapper;
import com.green.mapper.TagMapper;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

@Service
@Transactional
@Log4j
public class GameServiceImpl implements GameService{

	@Setter(onMethod_=@Autowired)
	private CompanyMapper cmapper;
	
	@Setter(onMethod_=@Autowired)
	private GameMapper mapper;
	
	@Setter(onMethod_=@Autowired)
	private TagMapper tmapper;
	
	@Setter(onMethod_=@Autowired)
	private SpecMapper smapper;
	
	@Setter(onMethod_=@Autowired)
	private GameResourceMapper rmapper;
	
	@Setter(onMethod_=@Autowired)
	private GamePictureMapper pmapper;
	
	@Setter(onMethod_=@Autowired)
	private RatingMapper ratingmapper;
	
		
	@Override
	public GameVO read(String title) {
		log.info("서비스에서 게임 조회 이름은 " + title );
		return mapper.read(title);
	}

	@Override
	public GameVO news(Long nno) {
		log.info("서비스에서 뉴스 조회 번호는 " + nno );
		return mapper.game(nno);
	}

	/*
	 * @Override public List<GameVO> getList(Criteria cri) {
	 * System.out.println("서비스에서 게임 목록 조회 "); return mapper.getList(); }
	 */

	@Override
	public boolean register(CompanyVO company, GameVO game, TagVO tag,  SpecVO spec, GameResourceVO resource, List<GamePictureVO> pictureList) {
		log.info("서비스에서 게임 데이터 추가");
		
		
		
		if(mapper.count(game.getTitle()) != 0) {
			return false;
		}
		
		Long cno = cmapper.getCno(company.getCompany());
		
		log.info("cno ->" + cno);
		if(cno == null) {
			
			cmapper.register(company);
		} else {
			game.setCno(cno);
		}
		
		
		mapper.register(game);
		smapper.register(spec);
		tmapper.register(tag);
		rmapper.register(resource);
		pictureList.forEach(i -> pmapper.register(i));
		
		return true;
	}

	@Override
	public boolean modify(CompanyVO company, GameVO game, TagVO tag, SpecVO spec, GameResourceVO resource, List<GamePictureVO> pictureList) {
		
		Long cno = cmapper.getCno(company.getCompany());
		
		if(cno == null) {
			
			cmapper.modify(company);
		} else {
			game.setCno(cno);
		}
		
		log.info("서비스에서 게임 데이터 수정");
		
		mapper.modify(game);
		smapper.modify(spec);
		tmapper.modify(tag);
		rmapper.modify(resource);
		pictureList.forEach(i -> pmapper.modify(i));
		
		return true;
		
	}

	@Override
	public boolean isExists(String title) {
		// TODO Auto-generated method stub
		return mapper.count(title) == 0 ? false : true;
	}

	@Override
	public boolean delete(Long gno) {
		// TODO Auto-generated method stub
		return mapper.delete(gno) == 1? true : false;
	}

	
	//평점
	@Override
	public void rating(RatingVO rating) {
		RatingVO result = ratingmapper.read(rating);
		System.out.println(result);
		if(result != null) {
			ratingmapper.update(rating);
		}else {
			ratingmapper.insert(rating);
		}
	}

	@Override
	public float rating_avg(Long gno) {
		float result = ratingmapper.rating_avg(gno);
		return result;
	}


}
