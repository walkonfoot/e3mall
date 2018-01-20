package cn.e3mall.service;

import cn.e3mall.common.pojo.EasyUIDataGridResult;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.pojo.TbItem;
import cn.e3mall.pojo.TbItemDesc;
import cn.e3mall.pojo.TbItemParamItem;

public interface ItemService {

	TbItem getItemById(long itemId);
	EasyUIDataGridResult getItemList(int page, int rows);
	E3Result addItem(TbItem item, String desc);
	TbItemDesc getItemDescById(long itemId);
	E3Result deleteItem(String ids);
	E3Result listItemDesc(long id);
	E3Result instockItem(String ids);
	E3Result reshelfItem(String ids);
	E3Result updateItem(TbItem item, String desc);
}
