package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jp.dataxeed.pm.entity.PartyEntity;
import com.jp.dataxeed.pm.form.party.SearchPartyForm;

public interface PartyMapper {

    // 全てのPartyEntityを取得
    List<PartyEntity> findAll();

    // 検索
    List<PartyEntity> searchParty(SearchPartyForm searchPartyForm);

    // id → partyEntity
    PartyEntity findById(int id);

    // insertToSave
    void insertToSave(PartyEntity partyEntity);

    // updateToSave
    void updateToSave(PartyEntity partyEntity);

    // deleteParty（理論削除）
    void deleteParty(int id);

    // === 追加 === コード → ID（完全一致、未削除）
    Integer selectIdByCode(@Param("code") String partyCode);

    // === 追加 === 名称 → ID（完全一致、未削除）
    Integer selectIdByName(@Param("name") String partyName);
}
