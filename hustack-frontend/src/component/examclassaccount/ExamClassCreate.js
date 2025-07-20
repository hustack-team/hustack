import React, {useState} from "react";
import {Grid, Stack, TextField} from "@mui/material";
import {useTranslation} from "react-i18next";
import {useForm} from "react-hook-form";
import {request} from "../../api";
import CustomizedDialogs from "../dialog/CustomizedDialogs";
import {errorNoti, successNoti} from "../../utils/notification";
import TertiaryButton from "../button/TertiaryButton";
import LoadingButton from "@mui/lab/LoadingButton";

const ExamClassCreate = ({open, onClose, onSuccess}) => {
  const {t} = useTranslation("common");
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    errors,
  } = useForm({
    defaultValues: {
      name: "",
      description: null
    }
  });

  const resetForm = () => {
    reset({
      name: "",
      description: null
    });
  };

  const onSubmit = (data) => {
    setLoading(true);
    request(
      "post",
      "/exam-classes",
      (res) => {
        successNoti(t("examClassCreatedSuccess"), 3000);
        resetForm();
        onSuccess && onSuccess(res.data);
        onClose();
      },
      {
        onError: () => {
          errorNoti(t("examClassCreateFailed"), 3000);
        }
      },
      data
    ).finally(() => {
      setLoading(false);
    });
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const content = (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <TextField
            inputRef={register({
              required: t("examClassNameRequired")
            })}
            fullWidth
            autoFocus
            size="small"
            id="name"
            name="name"
            label={t("examClassName") + " *"}
            variant="outlined"
            error={!!errors.name}
            helperText={errors.name?.message}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            inputRef={register}
            fullWidth
            size="small"
            id="description"
            name="description"
            label={t("description")}
            variant="outlined"
            multiline
            rows={3}
          />
        </Grid>
      </Grid>

      <Stack direction="row" spacing={1} justifyContent="center" sx={{mt: 3}}>
        <TertiaryButton
          color="inherit"
          onClick={handleClose}
          disabled={loading}
          type="button"
        >
          {t("common:cancel")}
        </TertiaryButton>
        <LoadingButton
          variant="contained"
          color="primary"
          type="submit"
          loading={loading}
          sx={{textTransform: 'none'}}
        >
          {t("save")}
        </LoadingButton>
      </Stack>
    </form>
  );

  return (
    <CustomizedDialogs
      open={open}
      title={t("add", {name: t("examClass")})}
      content={content}
      handleClose={handleClose}
      contentTopDivider
    />
  );
};

export default ExamClassCreate