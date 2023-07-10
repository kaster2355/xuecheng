package com.kaster.xuecheng.content.model.dto;

import com.kaster.xuecheng.content.model.po.Teachplan;
import com.kaster.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class TeachplanDto extends Teachplan {

    TeachplanMedia teachplanMedia;

    List<TeachplanDto> teachplanTreeNodes;
}
