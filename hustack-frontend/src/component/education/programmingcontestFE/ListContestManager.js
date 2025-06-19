import withScreenSecurity from "component/withScreenSecurity";
import {ListContestManagerByRegistration} from "./ListContestManagerByRegistration";
import {Paper} from "@mui/material";

function ListContestManager() {
  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
      <ListContestManagerByRegistration/>
      {/*<ListContestAll/>*/}
    </Paper>
  );
}

const screenName = "SCR_MANAGER_CONTEST_LIST";
export default withScreenSecurity(ListContestManager, screenName, true);
