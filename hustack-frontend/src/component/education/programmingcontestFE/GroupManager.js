import React, { useState } from "react";
import { AntTabs, AntTab, TabPanel, a11yProps } from "component/tab";
import withScreenSecurity from "component/withScreenSecurity";
import { useParams } from "react-router-dom";
import { Paper } from "@mui/material";
import GroupManagerListMember from "./GroupManagerListMember";

function GroupManager({ screenAuthorization }) {
  const { groupId } = useParams();
  const [selectedTab, setSelectedTab] = useState(0);

  const handleChange = (event, newValue) => {
    setSelectedTab(newValue);
  };

  return (
    <>
      <AntTabs
        value={selectedTab}
        onChange={handleChange}
        aria-label="group manager tabs"
        scrollButtons="auto"
        variant="scrollable"
      >
        <AntTab label="Members" {...a11yProps(0)} />
      </AntTabs>

      <TabPanel value={selectedTab} index={0} dir={"ltr"}>
        <Paper elevation={1} sx={{ padding: "16px 24px", borderRadius: 4 }}>
          <GroupManagerListMember groupId={groupId} screenAuthorization={screenAuthorization} />
        </Paper>
      </TabPanel>
    </>
  );
}

const screenName = "SCR_GROUP_MANAGER";
export default withScreenSecurity(GroupManager, screenName, true);