/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.millheat.internal.handler;

import static org.openhab.binding.millheat.internal.MillheatBindingConstants.*;

import java.time.ZoneId;
import java.util.Optional;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.millheat.internal.config.MillheatHomeConfiguration;
import org.openhab.binding.millheat.internal.dto.SetHolidayParameterRequest;
import org.openhab.binding.millheat.internal.model.Home;
import org.openhab.binding.millheat.internal.model.MillheatModel;
import org.openhab.binding.millheat.internal.model.ModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MillheatHomeHandler} is responsible for handling home commands, for now vacation mode properties
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class MillheatHomeHandler extends MillheatBaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(MillheatHomeHandler.class);
    private @NonNullByDefault({}) MillheatHomeConfiguration config;

    public MillheatHomeHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        handleCommand(channelUID, command, getMillheatModel());
    }

    @Override
    protected void handleCommand(final ChannelUID channelUID, final Command command, final MillheatModel model) {
        final Optional<Home> optionalHome = model.findHomeById(config.homeId);
        if (optionalHome.isPresent()) {
            updateStatus(ThingStatus.ONLINE);
            final Home home = optionalHome.get();
            if (CHANNEL_HOME_VACATION_TARGET_TEMPERATURE.equals(channelUID.getId())) {
                if (command instanceof RefreshType) {
                    updateState(channelUID, new QuantityType<>(home.getHolidayTemp(), SIUnits.CELSIUS));
                } else if (command instanceof QuantityType<?>) {
                    updateVacationModeProperty(home, SetHolidayParameterRequest.PROP_TEMP, command);
                } else if (command instanceof DecimalType) {
                    updateVacationModeProperty(home, SetHolidayParameterRequest.PROP_TEMP,
                            new QuantityType<Temperature>((DecimalType) command, SIUnits.CELSIUS));
                }
            } else if (CHANNEL_HOME_VACATION_MODE.equals(channelUID.getId())) {
                if (command instanceof RefreshType) {
                    updateState(channelUID, OnOffType.from(home.getMode().getMode() == ModeType.VACATION));
                } else if (command instanceof OnOffType) {
                    updateVacationModeProperty(home, SetHolidayParameterRequest.PROP_MODE, command);
                }
            } else if (CHANNEL_HOME_VACATION_MODE_ADVANCED.equals(channelUID.getId())) {
                if (command instanceof RefreshType) {
                    updateState(channelUID, OnOffType.from(home.isAdvancedVacationMode()));
                } else if (command instanceof OnOffType) {
                    updateVacationModeProperty(home, SetHolidayParameterRequest.PROP_MODE_ADVANCED, command);
                }
            } else if (CHANNEL_HOME_VACATION_MODE_START.equals(channelUID.getId())) {
                if (command instanceof RefreshType) {
                    if (home.getVacationModeStart() != null) {
                        updateState(channelUID,
                                new DateTimeType(home.getVacationModeStart().atZone(ZoneId.systemDefault())));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                } else if (command instanceof DateTimeType) {
                    updateVacationModeProperty(home, SetHolidayParameterRequest.PROP_START, command);
                }
            } else if (CHANNEL_HOME_VACATION_MODE_END.equals(channelUID.getId())) {
                if (command instanceof RefreshType) {
                    if (home.getVacationModeEnd() != null) {
                        updateState(channelUID,
                                new DateTimeType(home.getVacationModeEnd().atZone(ZoneId.systemDefault())));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                } else if (command instanceof DateTimeType) {
                    updateVacationModeProperty(home, SetHolidayParameterRequest.PROP_END, command);
                }
            } else {
                logger.debug("Received command {} on channel {}, but this channel is not handled or supported by {}",
                        channelUID.getId(), command.toString(), this.getThing().getUID());
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE);
        }
    }

    private void updateVacationModeProperty(Home home, String property, Command command) {
        getAccountHandler().ifPresent(handler -> {
            handler.updateVacationProperty(home, property, command);
        });
    }

    @Override
    public void initialize() {
        config = getConfigAs(MillheatHomeConfiguration.class);
        logger.debug("Initializing Millheat home using config {}", config);
        final Optional<Home> room = getMillheatModel().findHomeById(config.homeId);
        if (room.isPresent()) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }
}
