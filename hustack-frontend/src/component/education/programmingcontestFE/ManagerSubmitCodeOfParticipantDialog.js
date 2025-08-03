import React from "react";
import {Dialog, DialogContent, DialogTitle} from "@mui/material";
import TertiaryButton from "../../../button/TertiaryButton";
import {useTranslation} from "react-i18next";
import ManagerSubmitCodeOfParticipant from "./ManagerSubmitCodeOfParticipant";

export default function ManagerSubmitCodeOfParticipantDialog(props) {
  const { contestId, onClose, open } = props;
  const {t} = useTranslation("common");

  function handleClick() {
    onClose();
  }
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>Submissions</DialogTitle>
      <DialogContent>
        <ManagerSubmitCodeOfParticipant
          contestId={contestId}
          onClose={onClose}
        />
        <TertiaryButton color="inherit" onClick={handleClick}>{t('common:close')}</TertiaryButton>
      </DialogContent>
    </Dialog>
  );
}
