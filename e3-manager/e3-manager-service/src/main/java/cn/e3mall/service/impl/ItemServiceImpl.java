package cn.e3mall.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import cn.e3mall.common.jedis.JedisClient;
import cn.e3mall.common.pojo.EasyUIDataGridResult;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.common.utils.IDUtils;
import cn.e3mall.common.utils.JsonUtils;
import cn.e3mall.mapper.TbItemDescMapper;
import cn.e3mall.mapper.TbItemMapper;
import cn.e3mall.pojo.TbItem;
import cn.e3mall.pojo.TbItemDesc;
import cn.e3mall.pojo.TbItemDescExample;
import cn.e3mall.pojo.TbItemExample;
import cn.e3mall.pojo.TbItemExample.Criteria;
import cn.e3mall.pojo.TbItemParamItem;
import cn.e3mall.service.ItemService;

/**
 * 商品管理Service
 * 
 */
@Service
public class ItemServiceImpl implements ItemService {

	@Autowired
	private TbItemMapper itemMapper;
	@Autowired
	private TbItemDescMapper itemDescMapper;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Resource
	private Destination topicDestination;
	@Autowired
	private JedisClient jedisClient;
	
	@Value("${REDIS_ITEM_PRE}")
	private String REDIS_ITEM_PRE;
	@Value("${ITEM_CACHE_EXPIRE}")
	private Integer ITEM_CACHE_EXPIRE;
	@Value("${CONTENT_LIST}")
	private String CONTENT_LIST;
	
	@Override
	public TbItem getItemById(long itemId) {
		//查询缓存
		try {
			String json = jedisClient.get(REDIS_ITEM_PRE + ":" + itemId + ":BASE");
			if(StringUtils.isNotBlank(json)) {
				TbItem tbItem = JsonUtils.jsonToPojo(json, TbItem.class);
				return tbItem;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//缓存中没有，查询数据库
		//根据主键查询
		//TbItem tbItem = itemMapper.selectByPrimaryKey(itemId);
		TbItemExample example = new TbItemExample();
		Criteria criteria = example.createCriteria();
		//设置查询条件
		criteria.andIdEqualTo(itemId);
		//执行查询
		List<TbItem> list = itemMapper.selectByExample(example);
		if (list != null && list.size() > 0) {
			//把结果添加到缓存
			try {
				jedisClient.set(REDIS_ITEM_PRE + ":" + itemId + ":BASE", JsonUtils.objectToJson(list.get(0)));
				//设置过期时间
				jedisClient.expire(REDIS_ITEM_PRE + ":" + itemId + ":BASE", ITEM_CACHE_EXPIRE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list.get(0);
		}
		return null;
	}

	@Override
	public EasyUIDataGridResult getItemList(int page, int rows) {
		//设置分页信息
		PageHelper.startPage(page, rows);
		//执行查询
		TbItemExample example = new TbItemExample();
		List<TbItem> list = itemMapper.selectByExample(example);
		//创建一个返回值对象
		EasyUIDataGridResult result = new EasyUIDataGridResult();
		result.setRows(list);
		//取分页结果
		PageInfo<TbItem> pageInfo = new PageInfo<>(list);
		//取总记录数
		long total = pageInfo.getTotal();
		result.setTotal(total);
		return result;
	}

	@Override
	public E3Result addItem(TbItem item, String desc) {
		//生成商品id
		final long itemId = IDUtils.genItemId();
		//补全item的属性
		item.setId(itemId);
		//1-正常，2-下架，3-删除
		item.setStatus((byte) 1);
		item.setCreated(new Date());
		item.setUpdated(new Date());
		//向商品表插入数据
		itemMapper.insert(item);
		//创建一个商品描述表对应的pojo对象。
		TbItemDesc itemDesc = new TbItemDesc();
		//补全属性
		itemDesc.setItemId(itemId);
		itemDesc.setItemDesc(desc);
		itemDesc.setCreated(new Date());
		itemDesc.setUpdated(new Date());
		//向商品描述表插入数据
		itemDescMapper.insert(itemDesc);
		//添加缓存同步逻辑
		jedisClient.hdel(CONTENT_LIST, item.getId().toString());
		//发送商品添加消息
		jmsTemplate.send(topicDestination, new MessageCreator() {
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				TextMessage textMessage = session.createTextMessage(itemId + "");
				return textMessage;
			}
		});
		
		//返回成功
		return E3Result.ok();
	}

	@Override
	public TbItemDesc getItemDescById(long itemId) {
		//查询缓存
		try {
			String json = jedisClient.get(REDIS_ITEM_PRE + ":" + itemId + ":DESC");
			if(StringUtils.isNotBlank(json)) {
				TbItemDesc tbItemDesc = JsonUtils.jsonToPojo(json, TbItemDesc.class);
				return tbItemDesc;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		TbItemDesc itemDesc = itemDescMapper.selectByPrimaryKey(itemId);
		//把结果添加到缓存
		try {
			jedisClient.set(REDIS_ITEM_PRE + ":" + itemId + ":DESC", JsonUtils.objectToJson(itemDesc));
			//设置过期时间
			jedisClient.expire(REDIS_ITEM_PRE + ":" + itemId + ":DESC", ITEM_CACHE_EXPIRE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return itemDesc;
	}
	//删除商品
		@Override
		public E3Result deleteItem(String ids) {
			try {
				String[] idsArray = ids.split(",");
				List<Long> values = new ArrayList<Long>();
				for(String id : idsArray) {
					values.add(Long.parseLong(id));
				}
				TbItemExample e = new TbItemExample();
				TbItemExample.Criteria c = e.createCriteria();
				c.andIdIn(values);
			
				List<TbItem> list = itemMapper.selectByExample(e);
				if(list!=null && list.size()>0){
					TbItem item=list.get(0);
					item.setStatus((byte)3);
					itemMapper.updateByExample(item, e);
				}
				return E3Result.ok();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
			/*try {
				String[] idsArray = ids.split(",");
				List<Long> values = new ArrayList<Long>();
				for(String id : idsArray) {
					values.add(Long.parseLong(id));
				}
				TbItemExample e = new TbItemExample();
				TbItemExample.Criteria c = e.createCriteria();
				c.andIdIn(values);
				itemMapper.deleteByExample(e);
				
				
				TbItemDescExample de = new TbItemDescExample();
				cn.tf.taotao.po.TbItemDescExample.Criteria dc = de.createCriteria();
				dc.andItemIdIn(values);
				itemDescMapper.deleteByExample(de);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return E3Result.ok(); */
		}



		//加载商品描述
		@Override
		public E3Result listItemDesc(long id) {
			TbItemDesc result = itemDescMapper.selectByPrimaryKey(id);
			return E3Result.ok(result);
		}


		//更新
		@Override
		public E3Result updateItem(TbItem item, String desc) {
			try {
				//更新商品
				TbItemExample e = new TbItemExample();
				Criteria c = e.createCriteria();
				c.andIdEqualTo(item.getId());
				
				TbItem tbItem = itemMapper.selectByPrimaryKey(item.getId());
				item.setCreated(tbItem.getCreated());
				item.setUpdated(new Date());
				item.setStatus((byte)1);
				itemMapper.updateByExample(item, e);
				
				//更新商品描述
				
				TbItemDesc itemDesc = itemDescMapper.selectByPrimaryKey(item.getId());
				itemDesc.setCreated(tbItem.getCreated());
				itemDesc.setUpdated(new Date());
				itemDesc.setItemDesc(desc);
				itemDescMapper.updateByPrimaryKeySelective(itemDesc);
				//添加缓存同步逻辑
				jedisClient.hdel(CONTENT_LIST, item.getId().toString(),itemDesc.getItemId().toString());
				//更新规格
				/*E3Result result=updateItemParamItem(itemId, itemparam);
				if(result.getStatus()!=200){
					throw new Exception();
				}
				return E3Result.ok();*/
				
				
			} catch (Exception e) {
				e.printStackTrace();
				return E3Result.build(500, "更新商品失败");
			}
			return E3Result.ok();
			
		}


		//下架商品
		@Override
		public E3Result instockItem(String ids) {
			
			try {
				String[] idsArray = ids.split(",");
				List<Long> values = new ArrayList<Long>();
				for(String id : idsArray) {
					values.add(Long.parseLong(id));
				}
				TbItemExample e = new TbItemExample();
				TbItemExample.Criteria c = e.createCriteria();
				c.andIdIn(values);
			
				List<TbItem> list = itemMapper.selectByExample(e);
				if(list!=null && list.size()>0){
					TbItem item=list.get(0);
					item.setStatus((byte)2);
					itemMapper.updateByExample(item, e);
				}
				return E3Result.ok();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
		}



		@Override
		public E3Result reshelfItem(String ids) {
			try {
				String[] idsArray = ids.split(",");
				List<Long> values = new ArrayList<Long>();
				for(String id : idsArray) {
					values.add(Long.parseLong(id));
				}
				TbItemExample e = new TbItemExample();
				TbItemExample.Criteria c = e.createCriteria();
				c.andIdIn(values);
				List<TbItem> list = itemMapper.selectByExample(e);
				if(list!=null && list.size()>0){
					TbItem item=list.get(0);
					item.setStatus((byte)1);
					itemMapper.updateByExample(item, e);
				}
				return E3Result.ok();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		

}
