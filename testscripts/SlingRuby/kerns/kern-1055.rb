#!/usr/bin/env ruby


require 'nakamura/test'
include SlingUsers

class TC_Kern1055Test < Test::Unit::TestCase
  include SlingTest

  def test_user_search_after_deletion
    m = uniqueness()
    @s.switch_user(User.admin_user())
    user = create_user("testuser-#{m}", "Thurston", "Howell #{m}")
    wait_for_indexer()
    res = @s.execute_get(@s.url_for("/var/search/users.tidy.json?q=testuser-#{m}"))
    assert_equal("200", res.code, "Should have found user profile")
    json = JSON.parse(res.body)
    assert_equal(1, json["total"])
    assert_equal(true, @um.delete_user(user.name))
    res = @s.execute_get(@s.url_for(User.url_for(user.name) + ".json"))
    assert_equal("404", res.code, "Should have deleted Jackrabbit User")
    wait_for_indexer()
    res = @s.execute_get(@s.url_for("/var/search/users.tidy.json?q=testuser-#{m}"))
    assert_equal("200", res.code, "Search should be successful even if no matches")
    # We do not yet have any requirements as to what the search
    # results should be.
  end

  def test_group_search_after_deletion
    m = uniqueness()
    @s.switch_user(User.admin_user())
    group = create_group("testgroup-#{m}", "Test Group #{m}")
    wait_for_indexer()
    res = @s.execute_get(@s.url_for("/var/search/groups.tidy.json?q=testgroup-#{m}"))
    assert_equal("200", res.code, "Should have found group profile")
    json = JSON.parse(res.body)
    assert_equal(1, json["total"])
    assert_equal(true, @um.delete_group(group.name))
    res = @s.execute_get(@s.url_for(Group.url_for(group.name) + ".json"))
    assert_equal("404", res.code, "Should have deleted Jackrabbit Group")
    wait_for_indexer()
    res = @s.execute_get(@s.url_for("/var/search/groups.tidy.json?q=testgroup-#{m}"))
    assert_equal("200", res.code, "Search should be successful even if no matches")
    # We do not yet have any requirements as to what the search
    # results should be.
  end

end

