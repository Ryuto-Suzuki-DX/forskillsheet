package com.jp.dataxeed.pm.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jp.dataxeed.pm.dto.PartyCodeNameDto;
import com.jp.dataxeed.pm.entity.PartyEntity;
import com.jp.dataxeed.pm.form.party.PartyForm;
import com.jp.dataxeed.pm.form.party.SearchPartyForm;
import com.jp.dataxeed.pm.helper.PartyHelper;
import com.jp.dataxeed.pm.mapper.PartyMapper;

@Service
public class PartyService {

    private final PartyMapper partyMapper;
    private final PartyHelper partyHelper;

    @Autowired
    public PartyService(PartyMapper partyMapper, PartyHelper partyHelper) {
        this.partyMapper = partyMapper;
        this.partyHelper = partyHelper;
    }

    /**
     * 補完用の一覧を返す（id / partyCode / partyName）
     * - 既存 findAll() を使って PartyEntity -> PartyForm -> PartyCodeNameDto に変換
     * - PartyCodeNameDto に id を含める前提（フロントで hidden partyId を埋めるため）
     */
    public List<PartyCodeNameDto> getPartyCodeNameDtos() {
        return partyMapper.findAll().stream()
                .map(partyHelper::entityToForm) // PartyForm へ
                .map(form -> {
                    PartyCodeNameDto dto = new PartyCodeNameDto();
                    // ★ ここが重要：id も載せる（NOT NULL 制約回避のため hidden に流す）
                    dto.setId(form.getId());
                    dto.setPartyCode(form.getPartyCode());
                    dto.setPartyName(form.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 検索（null 受け取り時は空リストを返す）
     */
    public List<PartyForm> searchParty(SearchPartyForm searchPartyForm) {
        if (searchPartyForm == null) {
            return List.of();
        }
        return partyMapper.searchParty(searchPartyForm).stream()
                .map(partyHelper::entityToForm)
                .collect(Collectors.toList());
    }

    /**
     * id → PartyForm
     */
    public PartyForm findById(int id) {
        PartyEntity entity = partyMapper.findById(id);
        return partyHelper.entityToForm(entity);
    }

    /**
     * 保存（id の有無で insert / update）
     */
    public void saveParty(PartyForm partyForm) {
        PartyEntity partyEntity = partyHelper.formToEntity(partyForm);
        if (partyEntity == null) {
            System.out.println("partyFormがNull");
            return;
        }
        if (partyEntity.getId() == null) {
            partyMapper.insertToSave(partyEntity);
        } else {
            partyMapper.updateToSave(partyEntity);
        }
    }

    /**
     * 理論削除
     */
    public void deleteParty(int id) {
        partyMapper.deleteParty(id);
    }

    /**
     * party_code → id（完全一致・未削除）
     * - Controller 側の最終ガード（partyId 補完）用
     */
    public Integer findIdByCode(String partyCode) {
        if (partyCode == null || partyCode.isBlank())
            return null;
        return partyMapper.selectIdByCode(partyCode.trim());
    }

    /**
     * name → id（完全一致・未削除）
     * - Controller 側の最終ガード（partyId 補完）用
     */
    public Integer findIdByName(String partyName) {
        if (partyName == null || partyName.isBlank())
            return null;
        return partyMapper.selectIdByName(partyName.trim());
    }
}
