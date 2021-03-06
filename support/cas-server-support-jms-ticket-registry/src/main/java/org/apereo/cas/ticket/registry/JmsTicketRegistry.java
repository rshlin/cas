package org.apereo.cas.ticket.registry;

import org.apereo.cas.CipherExecutor;
import org.apereo.cas.StringBean;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.registry.queue.AddTicketMessageQueueCommand;
import org.apereo.cas.ticket.registry.queue.BaseMessageQueueCommand;
import org.apereo.cas.ticket.registry.queue.DeleteTicketMessageQueueCommand;
import org.apereo.cas.ticket.registry.queue.DeleteTicketsMessageQueueCommand;
import org.apereo.cas.ticket.registry.queue.UpdateTicketMessageQueueCommand;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jms.core.JmsTemplate;

/**
 * This is {@link JmsTicketRegistry}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
public class JmsTicketRegistry extends DefaultTicketRegistry {
    /**
     * Queue destination name.
     */
    public static final String QUEUE_DESTINATION = "CasJmsTicketRegistry";

    private final JmsTemplate jmsTemplate;
    private final StringBean id;

    public JmsTicketRegistry(final JmsTemplate jmsTemplate, final StringBean id) {
        this(jmsTemplate, id, CipherExecutor.noOp());
    }

    public JmsTicketRegistry(final JmsTemplate jmsTemplate, final StringBean id, final CipherExecutor cipherExecutor) {
        super(cipherExecutor);
        this.jmsTemplate = jmsTemplate;
        this.id = id;
    }

    @Override
    public void addTicket(final Ticket ticket) {
        super.addTicket(ticket);
        publishMessageToQueue(new AddTicketMessageQueueCommand(id, ticket));
    }

    @Override
    public boolean deleteSingleTicket(final String ticketId) {
        val result = super.deleteSingleTicket(ticketId);
        publishMessageToQueue(new DeleteTicketMessageQueueCommand(id, ticketId));
        return result;
    }

    @Override
    public long deleteAll() {
        val result = super.deleteAll();
        publishMessageToQueue(new DeleteTicketsMessageQueueCommand(id));
        return result;
    }

    @Override
    public Ticket updateTicket(final Ticket ticket) {
        val result = super.updateTicket(ticket);
        publishMessageToQueue(new UpdateTicketMessageQueueCommand(id, ticket));
        return result;
    }

    private void publishMessageToQueue(final BaseMessageQueueCommand cmd) {
        jmsTemplate.convertAndSend(QUEUE_DESTINATION, cmd,
            message -> {
                LOGGER.trace("Sending message [{}] from ticket registry id [{}]", message, cmd.getId());
                return message;
            });
    }
}
