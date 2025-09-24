package com.jp.dataxeed.pm.helper;

import org.springframework.stereotype.Component;

import com.jp.dataxeed.pm.entity.PartyEntity;
import com.jp.dataxeed.pm.form.party.PartyForm;

@Component
public class PartyHelper {

    // PartyEntity → PartyForm
    public PartyForm entityToForm(PartyEntity partyEntity) {
        PartyForm partyForm = new PartyForm();
        partyForm.setId(partyEntity.getId());
        partyForm.setPartyCode(partyEntity.getPartyCode());
        partyForm.setName(partyEntity.getName());
        partyForm.setAddress(partyEntity.getAddress());
        partyForm.setDetail(partyEntity.getDetail());
        partyForm.setAttention(partyEntity.getAttention());
        partyForm.setDeleteFlag(partyEntity.getDeleteFlag());
        partyForm.setCreatedAt(partyEntity.getCreatedAt());
        partyForm.setUpdatedAt(partyEntity.getUpdatedAt());
        return partyForm;
    }

    // PartyForm → PartyEntity
    public PartyEntity formToEntity(PartyForm partyForm) {
        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setId(partyForm.getId());
        partyEntity.setPartyCode(partyForm.getPartyCode());
        partyEntity.setName(partyForm.getName());
        partyEntity.setAddress(partyForm.getAddress());
        partyEntity.setDetail(partyForm.getDetail());
        partyEntity.setAttention(partyForm.getAttention());
        partyEntity.setDeleteFlag(partyForm.getDeleteFlag());
        partyEntity.setCreatedAt(partyForm.getCreatedAt());
        partyEntity.setUpdatedAt(partyForm.getUpdatedAt());
        return partyEntity;
    }

    // PartyEntity → PartyCodeNameDto

}
