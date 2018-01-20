package cn.e3mall.content.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import cn.e3mall.common.jedis.JedisClient;
import cn.e3mall.common.pojo.EasyUIDataGridResult;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.common.utils.JsonUtils;
import cn.e3mall.content.service.ContentService;
import cn.e3mall.mapper.TbContentMapper;
import cn.e3mall.pojo.TbContent;
import cn.e3mall.pojo.TbContentExample;
import cn.e3mall.pojo.TbContentExample.Criteria;
import cn.e3mall.pojo.TbItem;

/**
 * 内容管理Service
 * 
 */
@Service
public class ContentServiceImpl implements ContentService {

	@Autowired
	private TbContentMapper contentMapper;
	@Autowired
	private JedisClient jedisClient;
	
	@Value("${CONTENT_LIST}")
	private String CONTENT_LIST;
	
	@Override
	public E3Result addContent(TbContent content) {
		//将内容数据插入到内容表
		content.setCreated(new Date());
		content.setUpdated(new Date());
		//插入到数据库
		contentMapper.insert(content);
		//缓存同步,删除缓存中对应的数据。
		jedisClient.hdel(CONTENT_LIST, content.getCategoryId().toString());
		return E3Result.ok();
	}

	/**
	 * 根据内容分类id查询内容列表
	 * <p>Title: getContentListByCid</p>
	 * <p>Description: </p>
	 * @param cid
	 * @return
	 * @see cn.e3mall.content.service.ContentService#getContentListByCid(long)
	 */
	@Override
	public List<TbContent> getContentListByCid(long cid) {
		//查询缓存
		try {
			//如果缓存中有直接响应结果
			String json = jedisClient.hget(CONTENT_LIST, cid + "");
			if (StringUtils.isNotBlank(json)) {
				List<TbContent> list = JsonUtils.jsonToList(json, TbContent.class);
				return list;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//如果没有查询数据库
		TbContentExample example = new TbContentExample();
		Criteria criteria = example.createCriteria();
		//设置查询条件
		criteria.andCategoryIdEqualTo(cid);
		//执行查询
		List<TbContent> list = contentMapper.selectByExampleWithBLOBs(example);
		//把结果添加到缓存
		try {
			jedisClient.hset(CONTENT_LIST, cid + "", JsonUtils.objectToJson(list));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	//分页列表显示
		@Override
		public EasyUIDataGridResult getContentList(int page, int rows) {
			//设置分页信息
			PageHelper.startPage(page, rows);
			//执行查询
			TbContentExample example = new TbContentExample();
			List<TbContent> list = contentMapper.selectByExample(example);
			//创建一个返回值对象
			EasyUIDataGridResult result = new EasyUIDataGridResult();
			result.setRows(list);
			//取分页结果
			PageInfo<TbContent> pageInfo = new PageInfo<>(list);
			//取总记录数
			long total = pageInfo.getTotal();
			result.setTotal(total);
			return result;
		}

		//删除
		@Override
		public E3Result deleteContent(String ids) {
			try {
				String[] idsArray = ids.split(",");
				List<Long> values = new ArrayList<Long>();
				for(String id : idsArray) {
					values.add(Long.parseLong(id));
				}
				TbContentExample e = new TbContentExample();
				TbContentExample.Criteria c = e.createCriteria();
				c.andIdIn(values);
				contentMapper.deleteByExample(e);
				
				
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return E3Result.ok(); 
		}

		//修改
		@Override
		public E3Result updateContent(TbContent content) {
			
			try {
				//更新商品
				TbContentExample e = new TbContentExample();
				Criteria criteria = e.createCriteria();
				criteria.andIdEqualTo(content.getId());
				
				TbContent tbContent = contentMapper.selectByPrimaryKey(content.getId());
				
				content.setCreated(tbContent.getCreated());
				content.setUpdated(new Date());

				contentMapper.updateByExample(content, e);
				//添加缓存同步逻辑
				jedisClient.hdel(CONTENT_LIST, content.getCategoryId().toString());
				
			} catch (Exception e) {
				e.printStackTrace();
				return E3Result.build(500,"更新失败" );
			}
			return E3Result.ok();
			
		}
}
