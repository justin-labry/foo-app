/*
 * Copyright 2023-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.foo.app;

import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.core.ApplicationId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private ApplicationId appId;
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());
        log.info("foo.app Started");

        appId = coreService.registerApplication("org.foo.app");
        String routerInterfaceId = "Ethernet/32";
        int flowPriority = 777;
        DeviceId deviceId= null;
        try {
            deviceId = DeviceId.deviceId(new URI("device:leaf1"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        MacAddress srcMac = new MacAddress("0x90fb760098".getBytes());
        String outputPort = "Ethernet32";

        List<PiActionParam> actionParams = Lists.newArrayList(
                new PiActionParam(SaiConstants.SRC_MAC, srcMac.toBytes()));

        actionParams.add(new PiActionParam(SaiConstants.PORT, outputPort));

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .piTableAction(
                        PiAction.builder()
                                .withId(SaiConstants.INGRESS_ROUTING_SET_PORT_AND_SRC_MAC)
                                .withParameters(actionParams)
                                .build())
                .build();


        final PiCriterion.Builder rifIdCriterionBuilder = PiCriterion.builder();


        rifIdCriterionBuilder.matchExact(SaiConstants.HDR_ROUTER_INTERFACE_ID,
                routerInterfaceId);

        final TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchPi(rifIdCriterionBuilder.build())
                .build();

        PiAction piAction;

        flowRuleService.applyFlowRules(DefaultFlowRule.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .forTable(SaiConstants.INGRESS_ROUTING_ROUTER_INTERFACE_TABLE)
                .makePermanent()
                .withPriority(flowPriority)
                .forDevice(deviceId)
                .fromApp(appId)
                .build());
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("foo.app Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

}
