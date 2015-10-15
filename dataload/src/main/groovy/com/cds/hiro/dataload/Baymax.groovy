package com.cds.hiro.dataload

import com.cds.hiro.builders.Cda
import com.github.rahulsom.cda.POCDMT000040ClinicalDocument
import com.github.rahulsom.genealogy.Person
import com.github.rahulsom.geocoder.domain.Address
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator

/**
 * Created by rahul on 10/15/15.
 */
@Log4j
@CompileStatic
class Baymax {
  private HTTPBuilder client
  private String baseUrl

  Baymax(String url) {
    this.baseUrl = url
    this.client = new HTTPBuilder(url)
    client.headers['User-Agent'] = 'DataLoader'
  }

  Integer login(String username, String password) {
    log.debug "Logging in..."
    def login = [j_username: username, j_password: password, ajax: true]
    def loginRedirect = client.post(path: 'j_spring_security_check', body: login) { HttpResponseDecorator resp ->
      resp.allHeaders.find { it.name == 'Location' }?.value
    }
    if (!loginRedirect) {
      System.err.println "Could not login"
      System.exit(2)
    }

    def users = client.get(path: "user/search.json", query: [q: username])
    log.debug "Done logging in"
    def userList = users['list'] as List<Map>
    Integer.parseInt(userList.first().id.toString())
  }

  String createFacility(Facility facility, int idx) {
    def name = facility.nickName
    log.info "Creating facility ${name}"

    def postBody = facility.properties.
        collectEntries { k, v ->
          [k.toString().replace('_', '.'), v]
        }.findAll { k, v -> k != 'class' && v != null }

    def dbId = null
    def createResp = client.post(path: 'facility/save.json', body: postBody) { HttpResponseDecorator resp ->
      def json = new JsonSlurper().parse(resp.entity.content) as Map
      "${resp.statusLine.toString()}; dbId: ${json.id}; nickName: ${name}"
    }
    log.info "${'Create'.padLeft(20)} : ${createResp}"

    def identifier = "1.2.3.${idx}".toString()

    def configureResp = client.post(path: 'console/execute', body: [code: """
        import com.cds.healthdock.facilities.Facility
        import com.cds.healthdock.repository.*

        def f = Facility.findByNickName('${name}')
        f.hieViewEnabled = true
        f.normalizationEnabled = true
        f.ordersEnabled = true

        f.repositoryConfig = new RepositoryConfig(
            uniqueRepositoryId: '$identifier',
            xdsRegistryUrl: '${baseUrl}$name/registry'
        )

        f.pixPdqConfig = new iti.pixpdq.PixPdq(
            organizationOid: '$identifier',
            pixSender: '$identifier',
            receiverId: '$identifier'
        )

        f.assigningAuthority = new com.cds.healthdock.shared.AssigningAuthority(
            namespaceId: '${idx.toString()}',
            universalId: '$identifier',
            universalIdType: 'ISO'
        )

        f.addToIdentityDomains(new iti.pixpdq.IdentityDomain(
            assigningAuthority: new com.cds.healthdock.shared.AssigningAuthority(
                namespaceId: '${idx.toString()}',
                universalId: '$identifier',
                universalIdType: 'ISO'
            ),
            msh3: '${idx.toString()}',
            msh4: '${idx.toString()}',
            name: 'Default'
        ))

        f.save(flush: true)
        """]) { HttpResponseDecorator resp -> resp.statusLine.toString()
    }

    log.info "${'Configure'.padLeft(20)} : ${configureResp}"
    return identifier
  }

  void createPatient(Person person, Address address, String dob, String idntfr, Facility facility) {
    def nameS = "${person.lastName}^${person.firstName}"
    def idString = "${idntfr}^^^${facility.idx}&${facility.identifier}&ISO"
    def addrS = "${address.street}^^${address.city}^${address.state}^${address.zip}"

    def hl7 = """\
        |MSH|^~\\&|ABCDEFG&1.23.4&ISO|CDS|LABADT|MCM|20120109|SECURITY|ADT^A04|MSG00001|P|2.4
        |EVN|A01|198808181123
        |PID|||$idString||${nameS}||${dob}|${person.gender}||2106-3|${addrS}|GL||||S||ADT_PID18^2^M10||9-87654^NC""".
        stripMargin()
    def adtStatus = client.post(
        path: 'hl7Sender/send.json',
        body: [inMessage: hl7, host: 'localhost', port: facility.mllpPort_number],
        requestContentType: ContentType.URLENC
    ) { HttpResponseDecorator resp -> resp.statusLine }
    log.info "${'ADT'.padLeft(20)} : ${adtStatus}"

  }

  def addDocument(POCDMT000040ClinicalDocument document, Facility facility) {
    def docString = Cda.serialize(document)
    def status = client.post(
        path: "${facility.nickName}/ccd.json", body: docString, requestContentType: ContentType.XML
    ) { HttpResponseDecorator resp ->
      resp.statusLine
    }
    log.info "${'CCD'.padLeft(20)} : ${status}"
  }
}
