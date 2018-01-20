package cn.e3mall.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.e3mall.common.pojo.EasyUIDataGridResult;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.content.service.ContentService;
import cn.e3mall.pojo.TbContent;

/**
 * 内容管理Controller
 * 
 */
@Controller
public class ContentController {
	
	@Autowired
	private ContentService contentService;

	@RequestMapping(value="/content/save", method=RequestMethod.POST)
	@ResponseBody
	public E3Result addContent(TbContent content) {
		//调用服务把内容数据保存到数据库
		E3Result e3Result = contentService.addContent(content);
		return e3Result;
	}
	//加载列表
		@RequestMapping("/content/query/list")
		@ResponseBody
		public EasyUIDataGridResult getContentList(Integer page, Integer rows){
			EasyUIDataGridResult result = contentService.getContentList(page, rows);
			return result;
		}
		
		//删除
		@RequestMapping("/content/delete")
		@ResponseBody
		public E3Result deleteContent(String ids){
			return contentService.deleteContent(ids);
		}
		
		//更新
		@RequestMapping("/rest/content/edit")
		@ResponseBody
		public E3Result updateItem(TbContent content){
			E3Result result=contentService.updateContent(content);
			return result;
		}
}
