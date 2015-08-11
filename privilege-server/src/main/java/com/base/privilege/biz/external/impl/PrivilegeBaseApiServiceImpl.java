package com.base.privilege.biz.external.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.base.privilege.biz.external.IPrivilegeBaseApiService;
import com.base.privilege.mapper.ModuleMapper;
import com.base.privilege.mapper.ResourceMapper;
import com.base.privilege.mapper.UserMapper;
import com.base.privilege.model.Module;
import com.base.privilege.model.Resource;
import com.base.privilege.model.User;

@Service
public class PrivilegeBaseApiServiceImpl implements IPrivilegeBaseApiService {

	@Autowired
	private UserMapper userMapper;
	@Autowired
	private ModuleMapper moduleMapper;
	@Autowired
	private ResourceMapper resourceMapper;

	private final static String ICONS = "fa-gift、fa-cogs、fa-puzzle-piece、fa-sitemap、fa-table、fa-briefcase、fa-comments、fa-group、fa-globe、fa-th";

	@Override
	public User getUserByUsername(String username) {
		return userMapper.getUserByUsername(username);
	}

	@Override
	public String getModuleTree(Integer userId, String visitedModule, String visitedResource) {
		if (visitedModule == null)
			visitedModule = "";
		if (visitedResource == null)
			visitedResource = "";

		User user = userMapper.selectByPrimaryKey(userId);
		if (user == null)
			return "";

		StringBuilder sb = new StringBuilder();
		List<Module> moduleList;
		if (user.getIsAdmin()) {
			moduleList = moduleMapper.getModuleList();
		} else {
			moduleList = moduleMapper.getModuleListByUserId(userId);
		}
		if (moduleList == null || moduleList.size() == 0)
			return "";
		List<Resource> resourceList;
		if (user.getIsAdmin()) {
			resourceList = resourceMapper.getResourceListByType(1);
		} else {
			List<String> moduleFlags = new ArrayList<String>();
			for (Module m : moduleList) {
				moduleFlags.add(m.getFlag());
			}
			resourceList = resourceMapper.getResourceListByModuleFlag(moduleFlags, userId, 1);
		}

		// 从前一页面传过来的ModuleFlag标记和visitedStructure标记
		// moduleFlag = "p";
		// visitedStructure = "s-1-1-1";

		String[] icons = ICONS.split("、");
		int iconsFlag = 0;

		for (Module m : moduleList) {
			if (m.getFlag().equals(visitedModule)) {
				sb.append("<li class=\"active\">");
			} else {
				sb.append("<li>");
			}
			sb.append("<a href=\"javascript:;\">");
			sb.append("<i class=\"fa " + icons[iconsFlag++] + "\"></i>");
			sb.append("<span class=\"title\">");
			sb.append(" " + m.getName());
			sb.append("</span>");
			sb.append("<span class=\"selected\"></span>");
			sb.append("<span class=\"arrow open\"></span>");
			sb.append("</a>");
			sb.append(buildResourceTree(m, resourceList, "s", visitedModule, visitedResource));
			sb.append("</li>");
		}

		return sb.toString();
	}

	@Override
	public Integer updateUserById(User user) {
		return userMapper.updateByPrimaryKeySelective(user);
	}

	/**
	 * 
	 * @param m
	 *            ：当前正在遍历的模块
	 * @param resourceList
	 *            ：所有的资源列表
	 * @param structure
	 *            ：要对其进行树形结构的资源，根是s
	 * @param visitedModule
	 *            ：当前访问页面的Module
	 * @param visitedResource
	 *            ：当前访问页面的Resource
	 * @return
	 */
	private String buildResourceTree(Module m, List<Resource> resourceList, String structure, String visitedModule,
			String visitedResource) {
		if (resourceList == null || resourceList.size() == 0) {
			return "";
		}
		int parentLength = structure.split("-").length;
		// 计算一级子资源
		List<Resource> sonList = new ArrayList<Resource>();
		for (Resource r : resourceList) {
			if (r.getStructure().split("-").length == parentLength + 1 && r.getStructure().contains(structure)
					&& r.getModuleFlag().equals(m.getFlag())) {
				sonList.add(r);
			}
		}
		if (sonList.size() == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<ul class=\"sub-menu\">");
		for (Resource r : sonList) {

			if (r.getType() == 2) {
				continue;
			}

			// 递归，找子级的树形结构
			String s = buildResourceTree(m, resourceList, r.getStructure(), visitedModule, visitedResource);
			if (visitedResource.contains(r.getStructure()) && r.getModuleFlag().equals(visitedModule)) {
				sb.append("<li class=\"active\">");
			} else {
				sb.append("<li>");
			}
			if (s.equals("")) {
				sb.append("<a href=\"").append(m.getUrl()).append(r.getUrl()).append("?visitedModule=")
						.append(r.getModuleFlag()).append("&visitedResource=").append(r.getStructure()).append("\">");
				sb.append("<i class=\"fa fa-leaf \"></i>&nbsp;&nbsp;");
				sb.append(r.getName());
				sb.append("</a>");
			} else {
				sb.append("<a href=\"javascript:;\">");
				sb.append("<i class=\"fa fa-folder \"></i>&nbsp;&nbsp;");
				sb.append(r.getName());
				if (visitedResource.contains(r.getStructure()) && r.getModuleFlag().equals(visitedModule)) {
					sb.append("<span class=\"arrow open\"></span>");
				} else {
					sb.append("<span class=\"arrow\"></span>");
				}
				sb.append("</a>");
			}
			sb.append(s);
			sb.append("</li>");
		}
		sb.append("</ul>");
		return sb.toString();
	}

}
