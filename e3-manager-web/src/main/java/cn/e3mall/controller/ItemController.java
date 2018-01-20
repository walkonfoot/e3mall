package cn.e3mall.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.e3mall.common.pojo.EasyUIDataGridResult;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.pojo.TbItem;
import cn.e3mall.pojo.TbItemDesc;
import cn.e3mall.pojo.TbItemParamItem;
import cn.e3mall.service.ItemService;

/**
 * 商品管理Controller
 * 
 */
@Controller
public class ItemController {

	@Autowired
	private ItemService itemService;
	
	@RequestMapping("/item/{itemId}")
	@ResponseBody
	public TbItem getItemById(@PathVariable Long itemId) {
		TbItem tbItem = itemService.getItemById(itemId);
		return tbItem;
	}
	
	@RequestMapping("/item/list")
	@ResponseBody
	public EasyUIDataGridResult getItemList(Integer page, Integer rows) {
		//调用服务查询商品列表
		EasyUIDataGridResult result = itemService.getItemList(page, rows);
		return result;
	}
	
	/**
	 * 商品添加功能
	 */
	@RequestMapping(value="/item/save", method=RequestMethod.POST)
	@ResponseBody
	public E3Result addItem(TbItem item, String desc) {
		E3Result result = itemService.addItem(item, desc);
		return result;
	}
	//删除商品
		@RequestMapping("/rest/item/delete")
		@ResponseBody
		public E3Result  deleteItem(String ids){
			return itemService.deleteItem(ids);
			
		}
		
		//商品描述
		@RequestMapping("/rest/item/query/item/desc/{id}")
		@ResponseBody
		public E3Result listItemDesc(@PathVariable Long id) {
			return itemService.listItemDesc(id);
		}
		
		//更新
		@RequestMapping(value="/rest/item/update",method=RequestMethod.POST)
		@ResponseBody
		public E3Result updateItem(TbItem item, String desc){
			E3Result result=itemService.updateItem(item,desc);
			return result;
		}
		
		//下架
		@RequestMapping("/rest/item/instock")
		@ResponseBody
		public E3Result  instockItem(String ids){
			return itemService.instockItem(ids);
		}
		
		//上架
		@RequestMapping("/rest/item/reshelf")
		@ResponseBody
		public E3Result  reshelfItem(String ids){
			return itemService.reshelfItem(ids);	
		}
}
