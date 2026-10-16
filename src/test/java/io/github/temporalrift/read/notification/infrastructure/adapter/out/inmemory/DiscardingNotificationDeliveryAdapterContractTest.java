package io.github.temporalrift.read.notification.infrastructure.adapter.out.inmemory;

import io.github.temporalrift.read.contract.NotificationDeliveryContract;
import io.github.temporalrift.read.notification.domain.port.out.NotificationDeliveryPort;

class DiscardingNotificationDeliveryAdapterContractTest extends NotificationDeliveryContract {

    @Override
    protected NotificationDeliveryPort delivery() {
        return new DiscardingNotificationDeliveryAdapter();
    }
}
