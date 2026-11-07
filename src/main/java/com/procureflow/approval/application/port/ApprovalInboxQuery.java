package com.procureflow.approval.application.port;

import com.procureflow.approval.application.InboxItem;
import com.procureflow.shared.application.PageResult;
import java.util.Set;
import java.util.UUID;

public interface ApprovalInboxQuery {
    /** Current pending steps for these roles, never including the user's own requests. Oldest deadline first. */
    PageResult<InboxItem> pending(Set<String> roles, UUID currentUserId, int page, int size);
}